package com.codeinterview.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.codeinterview.ai.AiProperties;
import com.codeinterview.ai.LlmStreamClient;
import com.codeinterview.ai.UpstreamMsg;
import com.codeinterview.ai.UpstreamSseParser;
import com.codeinterview.mock.MockUpstreamServer;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.test.context.ActiveProfiles;

/**
 * T14 / 05 §10：mock 上游本身（18080 起停、按 model 切剧本、SSE 结构）+ 上游客户端联通。
 *
 * <p>用 mock profile 启动：application-mock.yml 把 base-url 指向 127.0.0.1:18080、api-key=mock，
 * 不会读取真实 Key，也不会请求真实上游。
 *
 * <p>注：05 §10.4 写的 {@code @SpringBootTest(profiles="mock")} 并非 Spring Boot 的属性，
 * 实际用 {@link ActiveProfiles}（已登记 02 §10）。
 */
@SpringBootTest
@ActiveProfiles("mock")
class MockUpstreamServerTest {

    @Autowired
    private MockUpstreamServer mockServer;
    @Autowired
    private LlmStreamClient llmStreamClient;
    @Autowired
    private UpstreamSseParser parser;
    @Autowired
    private AiProperties aiProperties;

    private String originalModel;

    @BeforeEach
    void setUp() {
        originalModel = aiProperties.getModel();
        mockServer.resetCounters();
    }

    @AfterEach
    void tearDown() {
        aiProperties.setModel(originalModel);
    }

    @Test
    void mockProfileStartsServerOn18080WithMockKey() {
        assertThat(mockServer.isRunning()).isTrue();
        assertThat(aiProperties.getBaseUrl()).isEqualTo("http://127.0.0.1:18080");
        assertThat(aiProperties.getApiKey()).isEqualTo("mock");
    }

    @Test
    void serverCanStartAndStopOnGivenPort() {
        int port = freePort();
        MockUpstreamServer standalone = new MockUpstreamServer(new ObjectMapper(), port);
        standalone.start();
        try {
            assertThat(standalone.isRunning()).isTrue();
        } finally {
            standalone.stop();
        }
        assertThat(standalone.isRunning()).isFalse();
    }

    @Test
    void okScriptStreams20DeltasThenUsageAndDone() throws Exception {
        List<UpstreamSseParser.ParseResult> results = readAndParse("mock-ok");

        List<String> deltas = results.stream().filter(UpstreamSseParser.ParseResult::hasDelta)
                .map(UpstreamSseParser.ParseResult::delta).toList();
        assertThat(deltas).hasSize(20);
        assertThat(String.join("", deltas)).contains("```java").contains("resize()");

        UpstreamSseParser.Usage usage = results.stream().filter(UpstreamSseParser.ParseResult::hasUsage)
                .map(UpstreamSseParser.ParseResult::usage).findFirst().orElseThrow();
        assertThat(usage.totalTokens()).isEqualTo(32);
        assertThat(usage.estimated()).isZero();
        assertThat(mockServer.hits("mock-ok")).isEqualTo(1);
    }

    @Test
    void requestBodyCarriesThinkingDisabledAndNoStreamOptions() throws Exception {
        readAndParse("mock-ok");

        String body = mockServer.lastRequestBody();
        assertThat(body).contains("\"thinking\":{\"type\":\"disabled\"}");
        assertThat(body).doesNotContain("stream_options");
    }

    @Test
    void nousageScriptDoesNotBreakAndReportsNoUsage() throws Exception {
        List<UpstreamSseParser.ParseResult> results = readAndParse("mock-nousage");

        assertThat(results.stream().filter(UpstreamSseParser.ParseResult::hasDelta)).hasSize(20);
        assertThat(results.stream().anyMatch(UpstreamSseParser.ParseResult::hasUsage)).isFalse();
    }

    @Test
    void mock500RetriesOnceBeforeFirstTokenAndThenSucceeds() throws Exception {
        List<UpstreamSseParser.ParseResult> results = readAndParse("mock-500");

        assertThat(mockServer.hits("mock-500")).isEqualTo(2);
        assertThat(results.stream().filter(UpstreamSseParser.ParseResult::hasDelta)).hasSize(20);
    }

    @Test
    void mock429FailsFastWithoutRetry() {
        aiProperties.setModel("mock-429");

        assertThatThrownBy(() -> llmStreamClient.open(List.of(new UpstreamMsg("user", "hi"))))
                .isInstanceOfSatisfying(LlmStreamClient.UpstreamHttpException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(429));
        assertThat(mockServer.hits("mock-429")).isEqualTo(1);
    }

    @Test
    void mockBreakEndsStreamWithoutDone() throws Exception {
        List<UpstreamSseParser.ParseResult> results = readAndParse("mock-break");

        assertThat(results).hasSize(3);
        assertThat(results.stream().filter(UpstreamSseParser.ParseResult::hasDelta)).hasSize(3);
        assertThat(results.stream().anyMatch(UpstreamSseParser.ParseResult::hasUsage)).isFalse();
    }

    @Test
    void http200WithBodyCodeNotZeroIsExposedAsUpstreamCode() throws Exception {
        List<UpstreamSseParser.ParseResult> results = readAndParse("mock-code");

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).code()).isEqualTo(11201);
        assertThat(results.get(0).hasDelta()).isFalse();
    }

    @Test
    void securitySuggestHideContinueSegmentIsSkippedButStreamContinues() throws Exception {
        List<UpstreamSseParser.ParseResult> results = readAndParse("mock-security");

        assertThat(results.stream().filter(UpstreamSseParser.ParseResult::securityHide)).hasSize(1);
        String content = results.stream().filter(UpstreamSseParser.ParseResult::hasDelta)
                .map(UpstreamSseParser.ParseResult::delta).reduce("", String::concat);
        assertThat(content).doesNotContain("不应该");
        assertThat(content).contains("HashMap");
        assertThat(results.stream().filter(UpstreamSseParser.ParseResult::hasDelta)).hasSize(20);
    }

    private List<UpstreamSseParser.ParseResult> readAndParse(String model) throws Exception {
        aiProperties.setModel(model);
        List<UpstreamSseParser.ParseResult> results = new ArrayList<>();
        InputStream stream = llmStreamClient.open(List.of(
                new UpstreamMsg("system", "system prompt"),
                new UpstreamMsg("user", "HashMap 的扩容机制？")));
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String payload = line.substring(5).trim();
                if ("[DONE]".equals(payload)) {
                    continue;
                }
                results.add(parser.parse(payload));
            }
        }
        return results;
    }

    private int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception ex) {
            throw new IllegalStateException("no free port", ex);
        }
    }
}
