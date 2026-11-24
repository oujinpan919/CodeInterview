package com.codeinterview.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.test.context.ActiveProfiles;

import com.codeinterview.support.ChatTestSupport;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * T22 / 08 §4.2 / D7：用户级模型配置 CRUD + 计费分叉。
 *
 * <p>核心断言：<b>用户自带 Key 时不计站点条数</b>（D7=(b)），
 * 站点/全局兜底仍然计入。
 *
 * <p>放在 chat 包（与既有 Chat*Test 一致），以便访问 {@code ChatTestSupport} 的 protected 成员。
 */
@Timeout(180)
@ActiveProfiles({"mock", "test"})
class AiModelConfigTest extends ChatTestSupport {

    private static final String MODELS_PATH = "/api/v1/ai/config/models";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    @Test
    @DisplayName("新增配置后列表可见，凭证只回显掩码")
    void createConfigThenListShowsMaskedCredential() throws Exception {
        TestUser user = createUser("cfg1");

        String body = """
                {
                  "displayName": "我的 DeepSeek",
                  "baseUrl": "http://127.0.0.1:%d",
                  "modelId": "mock-ok",
                  "apiKey": "ak-secret-key-123456",
                  "isEnabled": true
                }
                """.formatted(mockPort());
        JsonNode created = objectMapper.readTree(postJson(MODELS_PATH, user.token(), body, TIMEOUT).body())
                .path("data");

        String hint = created.path("apiKeyHint").asText();
        assertThat(hint).contains("****").doesNotContain("secret");
        assertThat(created.path("hasCredential").asBoolean()).isTrue();
        assertThat(created.path("testStatus").asText()).isEqualTo("untested");

        JsonNode list = objectMapper.readTree(getJson(MODELS_PATH, user.token()).body()).path("data");
        assertThat(list).hasSize(1);
        // 全列表响应也不得出现明文
        assertThat(list.toString()).doesNotContain("ak-secret-key-123456");
    }

    @Test
    @DisplayName("D7：用户自带 Key 时不消耗站点条数")
    void userOwnKeyDoesNotConsumeSiteQuota() throws Exception {
        TestUser user = createUser("cfg2");

        String body = """
                {
                  "displayName": "自带 Key",
                  "baseUrl": "http://127.0.0.1:%d",
                  "modelId": "mock-ok",
                  "apiKey": "mock-user-key",
                  "isEnabled": true,
                  "makeDefault": true
                }
                """.formatted(mockPort());
        postJson(MODELS_PATH, user.token(), body, TIMEOUT);

        int siteBefore = siteUsedCount();
        int userBefore = userUsedCount(user.id());

        String sse;
        try (SseStream stream = openChat(user.token(), chatBody("HashMap 的扩容机制？", null))) {
            sse = readAll(stream);
        }

        assertThat(sse).contains("event:done");
        // 账号额度照扣
        assertThat(userUsedCount(user.id())).isEqualTo(userBefore + 1);
        // D7：站点条数不变
        assertThat(siteUsedCount()).isEqualTo(siteBefore);

        JsonNode quota = eventData(sse, "done").path("quota");
        assertThat(quota.path("resolvedFrom").asText()).isEqualTo("USER_OWN");
        assertThat(quota.path("consumesSiteQuota").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("无用户配置时回退全局兜底，并计入站点条数")
    void fallsBackToGlobalAndConsumesSiteQuota() throws Exception {
        TestUser user = createUser("cfg3");
        int siteBefore = siteUsedCount();

        String sse;
        try (SseStream stream = openChat(user.token(), chatBody("HashMap 的扩容机制？", null))) {
            sse = readAll(stream);
        }

        assertThat(sse).contains("event:done");
        assertThat(siteUsedCount()).isEqualTo(siteBefore + 1);
        JsonNode quota = eventData(sse, "done").path("quota");
        assertThat(quota.path("resolvedFrom").asText()).isEqualTo("GLOBAL_FALLBACK");
        assertThat(quota.path("consumesSiteQuota").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("SSRF：内网 base_url 被拒绝，且不落库")
    void ssrfIsRejectedOnSave() throws Exception {
        TestUser user = createUser("cfg4");
        // mock profile 为连 127.0.0.1 上游放开了私有网段；这里临时关闭以验证 SSRF 防线
        boolean originalAllowPrivate = aiProperties.isAllowPrivateNetwork();
        aiProperties.setAllowPrivateNetwork(false);
        try {
            String body = """
                    {
                      "displayName": "内网探测",
                      "baseUrl": "http://192.168.1.1/v1",
                      "modelId": "gpt-4",
                      "apiKey": "x",
                      "isEnabled": true
                    }
                    """;
            HttpResult result = postJson(MODELS_PATH, user.token(), body, TIMEOUT);

            assertThat(result.status()).isEqualTo(400);
            assertThat(objectMapper.readTree(result.body()).path("code").asInt()).isEqualTo(40000);
            assertThat(objectMapper.readTree(getJson(MODELS_PATH, user.token()).body()).path("data")).isEmpty();
        } finally {
            aiProperties.setAllowPrivateNetwork(originalAllowPrivate);
        }
    }

    @Test
    @DisplayName("启用数量上限 5（D13）")
    void enabledCountIsCappedAtFive() throws Exception {
        TestUser user = createUser("cfg5");

        for (int i = 1; i <= 5; i++) {
            String body = """
                    {
                      "displayName": "模型%d",
                      "baseUrl": "http://127.0.0.1:%d",
                      "modelId": "mock-ok",
                      "apiKey": "k%d",
                      "isEnabled": true
                    }
                    """.formatted(i, mockPort(), i);
            assertThat(postJson(MODELS_PATH, user.token(), body, TIMEOUT).status()).isEqualTo(200);
        }

        String sixth = """
                {
                  "displayName": "模型6",
                  "baseUrl": "http://127.0.0.1:%d",
                  "modelId": "mock-ok",
                  "apiKey": "k6",
                  "isEnabled": true
                }
                """.formatted(mockPort());
        HttpResult result = postJson(MODELS_PATH, user.token(), sixth, TIMEOUT);

        assertThat(result.status()).isEqualTo(409);
        assertThat(objectMapper.readTree(result.body()).path("code").asInt()).isEqualTo(40902);
    }

    @Test
    @DisplayName("越权：不能读别人的配置（08 §7 S3）")
    void cannotReadOtherUsersConfig() throws Exception {
        TestUser owner = createUser("cfg6own");
        TestUser other = createUser("cfg6oth");

        String body = """
                {
                  "displayName": "私有配置",
                  "baseUrl": "http://127.0.0.1:%d",
                  "modelId": "mock-ok",
                  "apiKey": "owner-key",
                  "isEnabled": true
                }
                """.formatted(mockPort());
        JsonNode created = objectMapper.readTree(postJson(MODELS_PATH, owner.token(), body, TIMEOUT).body())
                .path("data");
        long id = created.path("id").asLong();

        HttpResult result = getJson(MODELS_PATH + "/" + id, other.token());
        assertThat(result.status()).isEqualTo(404);
    }

    @Test
    @DisplayName("未登录访问配置接口返回 401（40100）")
    void anonymousIsRejected() throws Exception {
        HttpResult result = getJson(MODELS_PATH, null);
        assertThat(result.status()).isEqualTo(401);
        assertThat(objectMapper.readTree(result.body()).path("code").asInt()).isEqualTo(40100);
    }

    // ---- v3.24 入口校验 ----

    @Test
    @DisplayName("v3.24：缺少展示名被 Bean Validation 拦截（40000），且不落库")
    void rejectsBlankDisplayName() throws Exception {
        TestUser user = createUser("cfg6");
        String body = """
                {
                  "displayName": "   ",
                  "baseUrl": "http://127.0.0.1:%d",
                  "modelId": "mock-ok",
                  "apiKey": "k"
                }
                """.formatted(mockPort());
        HttpResult result = postJson(MODELS_PATH, user.token(), body, TIMEOUT);
        assertThat(result.status()).isEqualTo(400);
        assertThat(objectMapper.readTree(result.body()).path("code").asInt()).isEqualTo(40000);
        assertThat(objectMapper.readTree(result.body()).path("message").asText()).contains("展示名");
        assertThat(objectMapper.readTree(getJson(MODELS_PATH, user.token()).body()).path("data")).isEmpty();
    }

    @Test
    @DisplayName("v3.24：接口地址缺少协议头被拦截（40000），提示需以 http(s) 开头")
    void rejectsBaseUrlWithoutScheme() throws Exception {
        TestUser user = createUser("cfg7");
        // 此前这种写法能通过 DTO 校验，直到服务层 urlGuard 才报错；现在入口即拦截
        String body = """
                {
                  "displayName": "无协议地址",
                  "baseUrl": "api.deepseek.com/v1",
                  "modelId": "deepseek-chat",
                  "apiKey": "k"
                }
                """;
        HttpResult result = postJson(MODELS_PATH, user.token(), body, TIMEOUT);
        assertThat(result.status()).isEqualTo(400);
        assertThat(objectMapper.readTree(result.body()).path("code").asInt()).isEqualTo(40000);
        assertThat(objectMapper.readTree(result.body()).path("message").asText()).contains("http");
    }

    @Test
    @DisplayName("v3.24：模型 ID 超长（>128）被拦截，避免写库截断")
    void rejectsTooLongModelId() throws Exception {
        TestUser user = createUser("cfg8");
        String body = """
                {
                  "displayName": "超长模型",
                  "baseUrl": "http://127.0.0.1:%d",
                  "modelId": "%s",
                  "apiKey": "k"
                }
                """.formatted(mockPort(), "m".repeat(129));
        HttpResult result = postJson(MODELS_PATH, user.token(), body, TIMEOUT);
        assertThat(result.status()).isEqualTo(400);
        assertThat(objectMapper.readTree(result.body()).path("code").asInt()).isEqualTo(40000);
        assertThat(objectMapper.readTree(result.body()).path("message").asText()).contains("模型 ID");
    }

    @Test
    @DisplayName("v3.24：同接口地址 + 同模型、不同参数允许并存（「严谨版/创意版」场景）")
    void allowsSameEndpointWithDifferentParams() throws Exception {
        TestUser user = createUser("cfg9");
        String strict = """
                {
                  "displayName": "DeepSeek 严谨",
                  "baseUrl": "http://127.0.0.1:%d/v1",
                  "modelId": "mock-ok",
                  "apiKey": "k1",
                  "temperature": 0.1,
                  "maxTokens": 600
                }
                """.formatted(mockPort());
        String creative = """
                {
                  "displayName": "DeepSeek 创意",
                  "baseUrl": "http://127.0.0.1:%d/v1",
                  "modelId": "mock-ok",
                  "apiKey": "k2",
                  "temperature": 0.9,
                  "maxTokens": 2000
                }
                """.formatted(mockPort());

        assertThat(postJson(MODELS_PATH, user.token(), strict, TIMEOUT).status()).isEqualTo(200);
        // 重复端点但参数不同：必须允许（前端只做「是否仍要新增」的软确认，后端不拦）
        assertThat(postJson(MODELS_PATH, user.token(), creative, TIMEOUT).status()).isEqualTo(200);
        assertThat(objectMapper.readTree(getJson(MODELS_PATH, user.token()).body()).path("data")).hasSize(2);
    }

    // ---- helpers ----

    /** mock 上游端口：ChatTestSupport 用随机端口，从 ai.base-url 里取。 */
    private int mockPort() {
        String baseUrl = aiProperties.getBaseUrl();
        return Integer.parseInt(baseUrl.substring(baseUrl.lastIndexOf(':') + 1));
    }
}
