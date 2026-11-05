package com.codeinterview.chat;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.codeinterview.support.ChatTestSupport;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * T15 / 05 §12：#5 首 token 超时（50202 + 配额回滚 + 可继续使用）、#13 usage 缺失 estimated=1。
 */
@Timeout(120)
class ChatQuotaTest extends ChatTestSupport {

    @Test
    void firstTokenTimeoutRollsBackQuotaAndStreamIsUsableAfterwards() throws Exception {
        aiProperties.setModel("mock-slow");
        TestUser user = createUser("quota5");

        long startedAt = System.currentTimeMillis();
        String sse;
        try (SseStream stream = openChat(user.token(), chatBody("慢上游", null))) {
            assertThat(stream.status()).isEqualTo(200);
            sse = readAll(stream);
        }
        long elapsed = System.currentTimeMillis() - startedAt;

        JsonNode error = eventData(sse, "error");
        assertThat(error).isNotNull();
        assertThat(error.path("code").asInt()).isEqualTo(50202);
        assertThat(sse).doesNotContain("event:done");
        // 20s 首 token 超时（不是 25s 上游才回）
        assertThat(elapsed).isBetween(15_000L, 24_000L);
        // 首 token 之前失败 → 两项配额回滚、日志 status=2
        assertThat(userUsedCount(user.id())).isZero();
        assertThat(latestChatLogStatus(user.id())).isEqualTo(2);
        assertThat(latestChatLogErrorCode(user.id())).isEqualTo(50202);

        // 可继续用：等过 3s 间隔后换 mock-ok 能正常出流
        Thread.sleep(3200);
        aiProperties.setModel("mock-ok");
        String retrySse;
        try (SseStream retry = openChat(user.token(), chatBody("再试一次", null))) {
            retrySse = readAll(retry);
        }
        assertThat(retrySse).contains("event:done");
        assertThat(userUsedCount(user.id())).isEqualTo(1);
    }

    @Test
    void missingUsageFallsBackToEstimateWithoutBreakingStream() throws Exception {
        aiProperties.setModel("mock-nousage");
        TestUser user = createUser("quota13");

        String sse;
        try (SseStream stream = openChat(user.token(), chatBody("没有 usage 的流", null))) {
            assertThat(stream.status()).isEqualTo(200);
            sse = readAll(stream);
        }

        assertThat(sse).contains("event:done");
        assertThat(countOccurrences(sse, "event:delta")).isEqualTo(20);
        JsonNode done = eventData(sse, "done");
        assertThat(done.path("usage").path("estimated").asInt()).isEqualTo(1);
        assertThat(done.path("usage").path("totalTokens").asInt()).isPositive();
        assertThat(latestChatLogEstimated(user.id())).isEqualTo(1);
        assertThat(latestChatLogStatus(user.id())).isEqualTo(1);
    }
}
