package com.codeinterview.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.codeinterview.ai.config.OutboundUrlGuard;
import com.codeinterview.common.exception.BusinessException;

/**
 * T20 / 08 §7 S1：SSRF 出站校验（B2 单表单方案下的强制补偿）。
 *
 * <p>纯单测，不启动 Spring：直接 new {@link OutboundUrlGuard}(false) 模拟生产配置。
 */
class OutboundUrlGuardTest {

    private final OutboundUrlGuard guard = new OutboundUrlGuard(false);

    @Test
    @DisplayName("正常公网地址通过，并去掉尾部斜杠")
    void publicUrlIsNormalized() {
        OutboundUrlGuard.Validation validation = guard.validate("https://api.deepseek.com/v1/");

        assertThat(validation.normalizedUrl()).isEqualTo("https://api.deepseek.com/v1");
        // 已有路径后缀，不提示补 /v1
        assertThat(validation.suggestV1()).isFalse();
    }

    @Test
    @DisplayName("无路径后缀时提示补 /v1（B2 最高频故障）")
    void bareHostSuggestsV1() {
        OutboundUrlGuard.Validation validation = guard.validate("https://api.deepseek.com");

        assertThat(validation.normalizedUrl()).isEqualTo("https://api.deepseek.com");
        assertThat(validation.suggestV1()).isTrue();
    }

    @Test
    @DisplayName("拒绝 localhost 与回环地址")
    void loopbackIsRejected() {
        assertThatThrownBy(() -> guard.validate("http://localhost/v1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("本机");
        assertThatThrownBy(() -> guard.validate("http://127.0.0.1:8080/v1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("内网或保留地址");
        assertThatThrownBy(() -> guard.validate("http://[::1]/v1"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("拒绝私有网段（含 10/172.16/192.168 与云元数据 169.254）")
    void privateRangesAreRejected() {
        for (String url : new String[]{
                "http://10.0.0.1/v1",
                "http://172.16.0.1/v1",
                "http://192.168.1.1/v1",
                "http://169.254.169.254/latest/meta-data",
                "http://100.64.0.1/v1",
                "http://localhost.localdomain/v1",
                "http://metadata.google.internal/v1"}) {
            assertThatThrownBy(() -> guard.validate(url))
                    .as("应拒绝 " + url)
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Test
    @DisplayName("只允许 http / https")
    void nonHttpSchemeIsRejected() {
        assertThatThrownBy(() -> guard.validate("ftp://example.com/v1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("http");
        assertThatThrownBy(() -> guard.validate("file:///etc/passwd"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("空地址 / 非法格式 / 无法解析的域名被拒绝")
    void malformedInputIsRejected() {
        assertThatThrownBy(() -> guard.validate("")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> guard.validate("   ")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> guard.validate("not-a-url")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> guard.validate("https://no-such-host-xyz-12345.invalid/v1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无法解析");
    }

    @Test
    @DisplayName("mock 模式放开私有网段（否则 127.0.0.1:18080 的上游连不上）")
    void testModeAllowsPrivateNetwork() {
        OutboundUrlGuard testGuard = new OutboundUrlGuard(true);

        assertThat(testGuard.validate("http://127.0.0.1:18080").normalizedUrl())
                .isEqualTo("http://127.0.0.1:18080");
        // 放开私有网段时局域网也可通过（仅测试便利）
        assertThat(testGuard.validate("http://192.168.1.1/v1").normalizedUrl())
                .isEqualTo("http://192.168.1.1/v1");
    }

    @Test
    @DisplayName("即使放开私有网段，云元数据地址仍必须被拦截")
    void metadataIsAlwaysRejectedEvenWhenPrivateAllowed() {
        OutboundUrlGuard testGuard = new OutboundUrlGuard(true);

        assertThatThrownBy(() -> testGuard.validate("http://169.254.169.254/latest/meta-data"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("元数据");
        // metadata.google.internal 在 BLOCKED_HOSTS 名单里，先被主机名规则拦下
        assertThatThrownBy(() -> testGuard.validate("http://metadata.google.internal/v1"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("chat/completions 拼接：不重复、已含则原样返回")
    void chatCompletionsUrlIsIdempotent() {
        assertThat(guard.chatCompletionsUrl("https://api.deepseek.com/v1"))
                .isEqualTo("https://api.deepseek.com/v1/chat/completions");
        assertThat(guard.chatCompletionsUrl("https://api.deepseek.com/v1/"))
                .isEqualTo("https://api.deepseek.com/v1/chat/completions");
        assertThat(guard.chatCompletionsUrl("https://api.deepseek.com/v1/chat/completions"))
                .isEqualTo("https://api.deepseek.com/v1/chat/completions");
    }
}
