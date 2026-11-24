package com.codeinterview.chat;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.test.context.ActiveProfiles;

import com.codeinterview.support.ChatTestSupport;
import com.codeinterview.user.entity.User;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * 05 §5.5：管理员（role=1）不受账号/全站「次数」限制，但间隔与并发仍生效。
 *
 * <p>把两个日额度都压到 1 条，保证「第二条还能成功」不能用额度余量解释；
 * 同时断言 admin 的调用不写 user_ai_quota / site_ai_usage，避免挤占其他用户预算。
 */
@Timeout(120)
@ActiveProfiles({"mock", "test"})
class ChatAdminQuotaTest extends ChatTestSupport {

    @Test
    void adminBypassesUserAndSiteQuotaWithoutCounting() throws Exception {
        aiProperties.getQuota().setUserDaily(1);
        aiProperties.getQuota().setSiteDaily(1);
        aiProperties.getLimit().setMinIntervalMs(0);
        TestUser admin = createUser("admin-quota", User.ROLE_ADMIN);

        HttpResult quota = getJson("/api/v1/ai/quota", admin.token());
        assertThat(quota.status()).isEqualTo(200);
        JsonNode quotaData = objectMapper.readTree(quota.body()).path("data");
        assertThat(quotaData.path("unlimited").asBoolean()).isTrue();
        assertThat(quotaData.path("dailyLimit").asInt()).isEqualTo(1);

        String first = readAll(openChat(admin.token(), chatBody("管理员第一条", null)));
        JsonNode firstDone = eventData(first, "done");
        assertThat(firstDone).isNotNull();
        assertThat(firstDone.path("quota").path("unlimited").asBoolean()).isTrue();

        // 普通用户在 userDaily=1 / siteDaily=1 下这里必然 429；admin 仍应成功
        String second = readAll(openChat(admin.token(), chatBody("管理员第二条", null)));
        assertThat(eventData(second, "done")).isNotNull();
        assertThat(second).doesNotContain("event:error");

        assertThat(userUsedCount(admin.id())).isZero();
        assertThat(siteUsedCount()).isZero();
        assertThat(latestChatLogStatus(admin.id())).isEqualTo(1);
    }

    @Test
    void normalUserStillBlockedByUserQuota() throws Exception {
        aiProperties.getQuota().setUserDaily(1);
        aiProperties.getQuota().setSiteDaily(1);
        aiProperties.getLimit().setMinIntervalMs(0);
        TestUser user = createUser("normal-quota");

        String first = readAll(openChat(user.token(), chatBody("普通用户第一条", null)));
        assertThat(eventData(first, "done")).isNotNull();
        assertThat(userUsedCount(user.id())).isEqualTo(1);

        HttpResult second = chatFail(user.token(), chatBody("普通用户第二条", null));
        assertThat(second.status()).isEqualTo(429);
        // 这里 minIntervalMs=0 且 userDaily=1：撞的是真实额度耗尽，仍是 42901
        assertThat(objectMapper.readTree(second.body()).path("code").asInt()).isEqualTo(42901);

        HttpResult quota = getJson("/api/v1/ai/quota", user.token());
        assertThat(objectMapper.readTree(quota.body()).path("data").path("unlimited").asBoolean()).isFalse();
    }

    @Test
    void adminStillSubjectToMinInterval() throws Exception {
        aiProperties.getQuota().setUserDaily(30);
        aiProperties.getQuota().setSiteDaily(300);
        aiProperties.getLimit().setMinIntervalMs(60_000);
        TestUser admin = createUser("admin-int", User.ROLE_ADMIN);

        String first = readAll(openChat(admin.token(), chatBody("间隔第一条", null)));
        assertThat(eventData(first, "done")).isNotNull();

        HttpResult second = chatFail(admin.token(), chatBody("间隔第二条", null));
        assertThat(second.status()).isEqualTo(429);
        JsonNode body = objectMapper.readTree(second.body());
        // v3.20 修复：管理员免限撞上间隔，应是「提问太快」42900，而非误报额度用尽 42901
        assertThat(body.path("code").asInt()).isEqualTo(42900);
        assertThat(body.path("message").asText()).contains("秒后再试");
        // 免限只免「次数」，不免审计：第一条仍然正常记账
        assertThat(latestChatLogStatus(admin.id())).isEqualTo(1);
        assertThat(userUsedCount(admin.id())).isZero();
    }
}
