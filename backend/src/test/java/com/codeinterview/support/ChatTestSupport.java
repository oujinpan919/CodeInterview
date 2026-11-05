package com.codeinterview.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ActiveProfiles;

import com.codeinterview.ai.AiProperties;
import com.codeinterview.chat.AiStreamRegistry;
import com.codeinterview.chat.ChatService;
import com.codeinterview.chat.RateGate;
import com.codeinterview.mock.MockUpstreamServer;
import com.codeinterview.user.UserService;
import com.codeinterview.user.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * T15 单测基类：真实 Tomcat（RANDOM_PORT）+ mock profile（不读真实 Key）。
 *
 * <p>为什么不用 MockMvc：SseEmitter 是长连接流，MockMvc 的 {@code asyncDispatch} 依赖
 * 容器重新派发，对 SSE 会永久等待（实测挂死 10 分钟）；改用真实 HTTP 客户端读取字节流，
 * 同时能真实验证「客户端 abort → 服务端停止、释放信号量」。
 *
 * <p>mock 上游在测试上下文里监听随机端口（ai.mock.port=0），避免与 T14 的 18080 上下文冲突。
 * 每个用例自建用户、自清 AI 三张表，不依赖前序用例的库内残留。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"ai.mock.port=0"})
@ActiveProfiles("mock")
public abstract class ChatTestSupport {

    protected static final String AI_CHAT_PATH = "/api/v1/ai/chat";
    protected static final String PASSWORD = "123456";

    @LocalServerPort
    protected int port;

    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Autowired
    protected AiProperties aiProperties;
    @Autowired
    protected RateGate rateGate;
    @Autowired
    protected MockUpstreamServer mockServer;
    @Autowired
    protected UserService userService;
    @Autowired
    protected ChatService chatService;
    @Autowired
    protected AiStreamRegistry registry;
    @Autowired
    protected ThreadPoolTaskExecutor aiExecutor;

    protected final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final List<Long> createdUserIds = new ArrayList<>();
    protected String originalModel;
    protected long originalMinIntervalMs;
    protected int originalUserConcurrency;
    protected int originalSiteConcurrency;
    protected boolean originalEnabled;
    protected String originalApiKey;

    @BeforeEach
    void resetState() {
        originalModel = aiProperties.getModel();
        originalMinIntervalMs = aiProperties.getLimit().getMinIntervalMs();
        originalUserConcurrency = aiProperties.getLimit().getUserConcurrency();
        originalSiteConcurrency = aiProperties.getLimit().getSiteConcurrency();
        originalEnabled = aiProperties.isEnabled();
        originalApiKey = aiProperties.getApiKey();
        jdbcTemplate.update("DELETE FROM user_ai_quota");
        jdbcTemplate.update("DELETE FROM site_ai_usage");
        jdbcTemplate.update("DELETE FROM ai_chat_log");
        rateGate.resetForTest();
        mockServer.resetCounters();
    }

    @AfterEach
    void clearState() {
        aiProperties.setModel(originalModel);
        aiProperties.getLimit().setMinIntervalMs(originalMinIntervalMs);
        aiProperties.getLimit().setUserConcurrency(originalUserConcurrency);
        aiProperties.getLimit().setSiteConcurrency(originalSiteConcurrency);
        aiProperties.setEnabled(originalEnabled);
        aiProperties.setApiKey(originalApiKey);
        rateGate.resetForTest();
        for (Long userId : createdUserIds) {
            jdbcTemplate.update("DELETE FROM `user` WHERE id = ?", userId);
        }
        createdUserIds.clear();
    }

    protected String baseUrl() {
        return "http://127.0.0.1:" + port;
    }

    /** 直接建用户（不走注册接口，避免注册限频干扰），再走真实登录拿 token。 */
    protected TestUser createUser(String prefix) throws Exception {
        String nickname = prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        User user = userService.create(nickname, PASSWORD, User.ROLE_NORMAL);
        createdUserIds.add(user.getId());
        String loginBody = objectMapper.writeValueAsString(Map.of("nickname", nickname, "password", PASSWORD));
        HttpResult login = postJson("/api/v1/auth/login", null, loginBody, Duration.ofSeconds(10));
        JsonNode data = objectMapper.readTree(login.body()).path("data");
        String token = data.path("token").asText();
        if (token.isBlank()) {
            throw new IllegalStateException("login failed for " + nickname + ": " + login.body());
        }
        return new TestUser(user.getId(), nickname, token);
    }

    protected HttpResult postJson(String path, String token, String body, Duration timeout) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl() + path))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(timeout)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        if (token != null) {
            builder.header("satoken", token);
        }
        HttpResponse<String> response = http.send(builder.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new HttpResult(response.statusCode(), response.body());
    }

    protected HttpResult getJson(String path, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl() + path))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .GET();
        if (token != null) {
            builder.header("satoken", token);
        }
        HttpResponse<String> response = http.send(builder.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new HttpResult(response.statusCode(), response.body());
    }

    /** 流开始前失败：返回 JSON（带 Accept: text/event-stream 模拟前端）。 */
    protected HttpResult chatFail(String token, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl() + AI_CHAT_PATH))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .header("X-Session-Id", "test-session")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        if (token != null) {
            builder.header("satoken", token);
        }
        HttpResponse<String> response = http.send(builder.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new HttpResult(response.statusCode(), response.body());
    }

    /** 正常建流：返回 SSE 字节流句柄（真实 HTTP，可 readAll 或 abort）。 */
    protected SseStream openChat(String token, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl() + AI_CHAT_PATH))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .header("X-Session-Id", "test-session")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        if (token != null) {
            builder.header("satoken", token);
        }
        HttpResponse<InputStream> response = http.send(builder.build(),
                HttpResponse.BodyHandlers.ofInputStream());
        return new SseStream(response.statusCode(), response.headers().firstValue("Content-Type").orElse(""),
                response.body());
    }

    protected String chatBody(String userMessage, Long questionId) throws Exception {
        return chatBody(List.of(message("user", userMessage)), questionId);
    }

    protected String chatBody(List<Map<String, String>> messages, Long questionId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("messages", messages);
        if (questionId != null) {
            body.put("questionId", questionId);
        }
        return objectMapper.writeValueAsString(body);
    }

    protected Map<String, String> message(String role, String content) {
        Map<String, String> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    protected int countOccurrences(String text, String keyword) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(keyword, index)) >= 0) {
            count++;
            index += keyword.length();
        }
        return count;
    }

    protected JsonNode eventData(String sse, String eventName) throws Exception {
        String normalized = sse.replace("\r\n", "\n");
        int eventIndex = normalized.indexOf("event:" + eventName + "\n");
        if (eventIndex < 0) {
            return null;
        }
        int dataIndex = normalized.indexOf("data:", eventIndex);
        if (dataIndex < 0) {
            return null;
        }
        int end = normalized.indexOf("\n\n", dataIndex);
        String payload = end < 0
                ? normalized.substring(dataIndex + 5).trim()
                : normalized.substring(dataIndex + 5, end).trim();
        return objectMapper.readTree(payload);
    }

    protected int userUsedCount(Long userId) {
        List<Integer> rows = jdbcTemplate.queryForList(
                "SELECT used_count FROM user_ai_quota WHERE user_id = ? AND stat_date = CURDATE()",
                Integer.class, userId);
        return rows.isEmpty() ? 0 : rows.get(0);
    }

    protected int latestChatLogStatus(Long userId) {
        List<Integer> rows = jdbcTemplate.queryForList(
                "SELECT status FROM ai_chat_log WHERE user_id = ? ORDER BY id DESC LIMIT 1", Integer.class, userId);
        return rows.isEmpty() ? -1 : rows.get(0);
    }

    protected Integer latestChatLogErrorCode(Long userId) {
        List<Integer> rows = jdbcTemplate.queryForList(
                "SELECT error_code FROM ai_chat_log WHERE user_id = ? ORDER BY id DESC LIMIT 1", Integer.class, userId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    protected int latestChatLogEstimated(Long userId) {
        List<Integer> rows = jdbcTemplate.queryForList(
                "SELECT estimated FROM ai_chat_log WHERE user_id = ? ORDER BY id DESC LIMIT 1", Integer.class, userId);
        return rows.isEmpty() ? 0 : rows.get(0);
    }

    protected String readAll(SseStream stream) throws IOException {
        try (stream) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = stream.reader().readLine()) != null) {
                content.append(line).append('\n');
            }
            return content.toString();
        }
    }

    protected record TestUser(Long id, String nickname, String token) {
    }

    protected record HttpResult(int status, String body) {
    }

    /** SSE 流句柄：可读完，也可直接 close 模拟客户端停止。 */
    public static final class SseStream implements AutoCloseable {

        private final int status;
        private final String contentType;
        private final InputStream inputStream;
        private final BufferedReader reader;

        SseStream(int status, String contentType, InputStream inputStream) {
            this.status = status;
            this.contentType = contentType;
            this.inputStream = inputStream;
            this.reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        }

        public int status() {
            return status;
        }

        public String contentType() {
            return contentType;
        }

        public BufferedReader reader() {
            return reader;
        }

        /** 读到某关键字（含该行）后返回已收内容，用于「已出字后停止」。 */
        public String readUntil(String keyword) throws IOException {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n');
                if (line.contains(keyword)) {
                    return content.toString();
                }
            }
            return content.toString();
        }

        @Override
        public void close() throws IOException {
            inputStream.close();
        }
    }
}
