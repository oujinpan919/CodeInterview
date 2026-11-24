package com.codeinterview.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.codeinterview.ai.AiProperties;
import com.codeinterview.ai.LlmStreamClient;
import com.codeinterview.ai.PromptBuilder;
import com.codeinterview.ai.UpstreamCodeMapper;
import com.codeinterview.ai.UpstreamSseParser;
import com.codeinterview.chat.dto.ChatReq;
import com.codeinterview.chat.dto.Msg;
import com.codeinterview.common.exception.BusinessException;
import com.codeinterview.common.result.ResultCode;
import com.codeinterview.support.ChatTestSupport;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * T15 / 05 §12：#1 未登录、#3 账号额度用尽、#4 全站并发满、#8 上下文静默裁剪、
 * #11 同账号并发、#12 3s 间隔、#15 线程池拒绝。
 */
@Timeout(120)
@ActiveProfiles({"mock", "test"})
class ChatGateTest extends ChatTestSupport {

    @Test
    void unauthenticatedRequestReturns401JsonWithoutStream() throws Exception {
        HttpResult result = chatFail(null, chatBody("hi", null));

        assertThat(result.status()).isEqualTo(401);
        assertThat(result.body()).doesNotContain("event:");
        assertThat(objectMapper.readTree(result.body()).path("code").asInt()).isEqualTo(40100);
    }

    @Test
    void contextOverLimitIsSilentlyTrimmedInsteadOfRejected() throws Exception {
        TestUser user = createUser("gate8");
        StringBuilder chunk = new StringBuilder();
        for (int i = 0; i < 700; i++) {
            chunk.append('题');
        }
        List<Map<String, String>> messages = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            messages.add(message(i % 2 == 0 ? "user" : "assistant", chunk.toString()));
        }
        messages.add(message("user", "最后一条用户消息（必须保留）"));

        try (SseStream stream = openChat(user.token(), chatBody(messages, null))) {
            assertThat(stream.status()).isEqualTo(200);
            assertThat(stream.contentType()).contains("text/event-stream");
            String sse = readAll(stream);
            assertThat(sse).contains("event:done");
        }

        JsonNode upstreamMessages = objectMapper.readTree(mockServer.lastRequestBody()).path("messages");
        assertThat(upstreamMessages.get(0).path("role").asText()).isEqualTo("system");
        int clientCount = 0;
        int clientChars = 0;
        String lastContent = "";
        for (JsonNode node : upstreamMessages) {
            if ("system".equals(node.path("role").asText())) {
                continue;
            }
            clientCount++;
            clientChars += node.path("content").asText().length();
            lastContent = node.path("content").asText();
        }
        assertThat(clientCount).isLessThanOrEqualTo(10);
        assertThat(clientChars).isLessThanOrEqualTo(6000);
        assertThat(lastContent).isEqualTo("最后一条用户消息（必须保留）");
    }

    @Test
    void userQuotaExhaustedReturns42901AndQuotaEndpointStaysInSync() throws Exception {
        TestUser user = createUser("gate3");
        jdbcTemplate.update("INSERT INTO user_ai_quota (user_id, stat_date, used_count) VALUES (?, CURDATE(), 30)",
                user.id());

        HttpResult result = chatFail(user.token(), chatBody("hi", null));
        assertThat(result.status()).isEqualTo(429);
        assertThat(objectMapper.readTree(result.body()).path("code").asInt()).isEqualTo(42901);
        assertThat(userUsedCount(user.id())).isEqualTo(30);

        HttpResult quotaResult = getJson("/api/v1/ai/quota", user.token());
        JsonNode quota = objectMapper.readTree(quotaResult.body()).path("data");
        assertThat(quota.path("used").asInt()).isEqualTo(30);
        assertThat(quota.path("dailyLimit").asInt()).isEqualTo(30);
        assertThat(quota.path("remaining").asInt()).isZero();
        assertThat(quota.path("resetAt").asText()).isNotBlank();
    }

    @Test
    void sameAccountConcurrencyIsLimited() throws Exception {
        aiProperties.getLimit().setMinIntervalMs(0);
        TestUser user = createUser("gate11");

        try (SseStream first = openChat(user.token(), chatBody("first", null))) {
            assertThat(first.status()).isEqualTo(200);
            HttpResult second = chatFail(user.token(), chatBody("second", null));
            assertThat(second.status()).isEqualTo(429);
            // v3.20 修复：账号已有流在跑是「稍等」而非「额度用尽」，应为 42900
            JsonNode body = objectMapper.readTree(second.body());
            assertThat(body.path("code").asInt()).isEqualTo(42900);
            assertThat(body.path("message").asText()).contains("还在生成中");
            assertThat(userUsedCount(user.id())).isEqualTo(1);
            assertThat(readAll(first)).contains("event:done");
        }
    }

    @Test
    void siteConcurrencyFullRejectsFifthStreamWith42902() throws Exception {
        aiProperties.getLimit().setMinIntervalMs(0);
        List<TestUser> users = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            users.add(createUser("gate4"));
        }
        List<SseStream> running = new ArrayList<>();
        try {
            for (int i = 0; i < 4; i++) {
                SseStream stream = openChat(users.get(i).token(), chatBody("concurrent " + i, null));
                assertThat(stream.status()).isEqualTo(200);
                running.add(stream);
            }

            HttpResult fifth = chatFail(users.get(4).token(), chatBody("fifth", null));
            assertThat(fifth.status()).isEqualTo(429);
            assertThat(objectMapper.readTree(fifth.body()).path("code").asInt()).isEqualTo(42902);
            // 全站并发闸门在预扣之前：第 5 条不扣任何额度
            assertThat(userUsedCount(users.get(4).id())).isZero();

            for (SseStream stream : running) {
                assertThat(readAll(stream)).contains("event:done");
            }
        } finally {
            for (SseStream stream : running) {
                stream.close();
            }
        }
    }

    @Test
    void secondRequestWithin3sReturnsRateLimitedNotQuotaExhausted() throws Exception {
        TestUser user = createUser("gate12");
        try (SseStream first = openChat(user.token(), chatBody("first", null))) {
            assertThat(first.status()).isEqualTo(200);
            HttpResult second = chatFail(user.token(), chatBody("second", null));
            assertThat(second.status()).isEqualTo(429);
            // v3.20 修复：3s 间隔未到是「提问太快」而非「额度用尽」，应为 42900 且给出秒数
            JsonNode body = objectMapper.readTree(second.body());
            assertThat(body.path("code").asInt()).isEqualTo(42900);
            assertThat(body.path("message").asText()).contains("秒后再试");
            assertThat(body.path("message").asText()).doesNotContain("明天");
            assertThat(userUsedCount(user.id())).isEqualTo(1);
            assertThat(readAll(first)).contains("event:done");
        }
    }

    @Test
    void threadPoolRejectionReturns42902AndRollsBackBothQuotas() {
        ChatValidator validator = mock(ChatValidator.class);
        when(validator.validateAndTrim(any())).thenReturn(List.of(userMessage()));
        QuotaService quotaService = mock(QuotaService.class);
        when(quotaService.tryConsumeUser(anyLong(), any(), anyInt())).thenReturn(true);
        when(quotaService.tryConsumeSite(any(), anyInt())).thenReturn(true);
        RateGate rateGate = mock(RateGate.class);
        when(rateGate.tryAcquire(anyLong())).thenReturn(RateGate.GateResult.ACQUIRED);
        AiStreamRegistry registry = mock(AiStreamRegistry.class);
        when(registry.register(any(), any())).thenReturn(new AiStreamRegistry.StreamState("m1", new SseEmitter()));
        ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);
        doThrow(new RejectedExecutionException("pool full")).when(executor).execute(any());

        AiProperties props = new AiProperties();
        props.setEnabled(true);
        props.setApiKey("mock");
        ChatService service = new ChatService(validator, mock(PromptBuilder.class), mock(LlmStreamClient.class),
                mock(UpstreamSseParser.class), mock(UpstreamCodeMapper.class), quotaService, rateGate, registry,
                mock(AiChatLogService.class), props, executor,
                stubResolver(props), mock(com.codeinterview.ai.service.CredentialService.class),
                mock(com.codeinterview.ai.service.ConversationService.class));

        assertThatThrownBy(() -> service.chat(1L, new ChatReq(), false))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getResultCode()).isEqualTo(ResultCode.AI_SITE_LIMIT));
        verify(quotaService).rollbackSite(any());
        verify(quotaService).rollbackUser(anyLong(), any());
        verify(rateGate).release(1L);
        // 不建流：已登记的流必须摘掉，避免 watchdog 稍后重复回滚/写假审计
        verify(registry).unregister(any());
    }

    @Test
    void disabledSwitchAndEmptyKeyReturn50201WithoutCallingUpstream() throws Exception {
        TestUser user = createUser("gate-sw");

        aiProperties.setEnabled(false);
        HttpResult disabled = chatFail(user.token(), chatBody("hi", null));
        assertThat(disabled.status()).isEqualTo(502);
        assertThat(objectMapper.readTree(disabled.body()).path("code").asInt()).isEqualTo(50201);
        assertThat(mockServer.totalRequests()).isZero();

        aiProperties.setEnabled(true);
        aiProperties.setApiKey("   ");
        HttpResult noKey = chatFail(user.token(), chatBody("hi", null));
        assertThat(noKey.status()).isEqualTo(502);
        assertThat(objectMapper.readTree(noKey.body()).path("code").asInt()).isEqualTo(50201);
        assertThat(mockServer.totalRequests()).isZero();
    }

    private Msg userMessage() {
        Msg msg = new Msg();
        msg.setRole("user");
        msg.setContent("hi");
        return msg;
    }

    /** 解析链 stub：返回 GLOBAL_FALLBACK（与旧行为一致，消耗站点配额）。 */
    private com.codeinterview.ai.config.ModelConfigResolver stubResolver(AiProperties props) {
        com.codeinterview.ai.config.ModelConfigResolver resolver =
                mock(com.codeinterview.ai.config.ModelConfigResolver.class);
        when(resolver.resolve(anyLong(), any())).thenReturn(
                new com.codeinterview.ai.config.ModelConfigResolver.Resolved(
                        0L,
                        com.codeinterview.ai.config.ModelConfigResolver.ResolvedFrom.GLOBAL_FALLBACK,
                        "mock", "全局兜底", props.getBaseUrl(), "mock-ok", null,
                        props.getTemperature(), props.getMaxTokens(), false, null, null));
        return resolver;
    }
}
