package com.codeinterview.ai.config;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.codeinterview.ai.AiProperties;
import com.codeinterview.common.exception.BusinessException;
import com.codeinterview.common.result.ResultCode;

import lombok.extern.slf4j.Slf4j;

/**
 * 出站目标校验（08 §7 S1 / S7）：B2 单表单方案下用户可填任意 base_url，
 * SSRF 是本次新增的最大攻击面，保存与测试两处都必须过这一关。
 *
 * <p>三道检查：
 * <ol>
 *   <li>协议白名单：只允许 http / https；</li>
 *   <li>字面量黑名单：拒绝 localhost / 元数据域名；</li>
 *   <li><b>DNS 解析后二次校验</b>：解析出的每个地址都不得落在私有/回环/链路本地段，
 *       防止 {@code 127.0.0.1.nip.io} 这类 DNS rebinding 绕过字面量检查。</li>
 * </ol>
 *
 * <p>同时做 {@code /v1} 规范化提示——这是 LobeChat 等产品文档里的最高频故障，
 * B2 全手工填 URL 时必须主动纠错。
 */
@Slf4j
@Component
public class OutboundUrlGuard {

    private static final String[] BLOCKED_HOSTS = {
            "localhost", "localhost.localdomain", "ip6-localhost", "ip6-loopback", "metadata.google.internal"
    };

    private final boolean allowPrivateNetwork;
    /** 动态读取：测试中切换 ai.allow-private-network 后立即生效。 */
    private final AiProperties props;

    /** 生产用：读 ai.allow-private-network（默认 false）。 */
    @org.springframework.beans.factory.annotation.Autowired
    public OutboundUrlGuard(AiProperties props) {
        this.allowPrivateNetwork = props.isAllowPrivateNetwork();
        this.props = props;
    }

    /** 供测试注入：允许私有网段（mock 上游跑在 127.0.0.1）。 */
    public OutboundUrlGuard(boolean allowPrivateNetwork) {
        this.allowPrivateNetwork = allowPrivateNetwork;
        this.props = null;
    }

    /** 当前是否放开私有网段（构造参数优先，其次实时读配置）。 */
    private boolean privateNetworkAllowed() {
        return props != null ? props.isAllowPrivateNetwork() : allowPrivateNetwork;
    }

    /** 校验结果：规范化后的 URL + 是否需要提示补 /v1。 */
    public record Validation(String normalizedUrl, boolean suggestV1) {
    }

    /**
     * 保存与测试前调用。抛 {@link BusinessException}(40000) 表示不允许。
     *
     * @param rawUrl 用户填写的 base_url
     */
    public Validation validate(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw reject("接口地址不能为空");
        }
        String trimmed = rawUrl.trim();

        URI uri;
        try {
            uri = new URI(trimmed);
        } catch (URISyntaxException ex) {
            throw reject("接口地址格式不正确");
        }

        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw reject("接口地址只支持 http / https");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw reject("接口地址缺少主机名");
        }

        String lowerHost = host.toLowerCase(Locale.ROOT);
        for (String blocked : BLOCKED_HOSTS) {
            if (lowerHost.equals(blocked) || lowerHost.endsWith("." + blocked)) {
                throw reject("不允许连接本机或元数据地址");
            }
        }

        if (!privateNetworkAllowed()) {
            assertPublicHost(lowerHost);
        } else {
            // 放开私有网段（仅 mock/测试）时，云元数据地址仍必须拦截：
            // 169.254.169.254 等高价值目标不能因测试便利而被放行。
            assertNoMetadataTarget(lowerHost);
        }

        String path = uri.getPath() == null ? "" : uri.getPath();
        String normalizedPath = stripTrailingSlash(path);
        boolean suggestV1 = normalizedPath.isEmpty();

        String normalized = scheme.toLowerCase(Locale.ROOT) + "://" + host
                + (uri.getPort() > 0 ? ":" + uri.getPort() : "")
                + normalizedPath;

        log.debug("[ai-config] outbound url ok: {} -> {} suggestV1={}", trimmed, normalized, suggestV1);
        return new Validation(normalized, suggestV1);
    }

    /**
     * 即使放开了私有网段，也禁止云厂商元数据地址。
     * 覆盖：169.254.0.0/16（AWS/GCP/Azure 元数据段）与 metadata.* 主机名。
     */
    private void assertNoMetadataTarget(String host) {
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                byte[] octets = address.getAddress();
                if (octets.length == 4) {
                    int first = octets[0] & 0xFF;
                    int second = octets[1] & 0xFF;
                    if (first == 169 && second == 254) {
                        throw reject("不允许连接云元数据地址");
                    }
                }
            }
        } catch (UnknownHostException ex) {
            throw reject("接口地址的域名无法解析");
        }
    }

    /** DNS 解析后逐个地址校验，防 DNS rebinding。 */
    private void assertPublicHost(String host) {        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException ex) {
            throw reject("接口地址的域名无法解析");
        }
        if (addresses.length == 0) {
            throw reject("接口地址的域名无法解析");
        }
        for (InetAddress address : addresses) {
            if (isBlockedAddress(address)) {
                throw reject("接口地址指向内网或保留地址，不允许连接");
            }
        }
    }

    private boolean isBlockedAddress(InetAddress address) {
        if (address.isLoopbackAddress() || address.isAnyLocalAddress()
                || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        if (address instanceof Inet4Address) {
            byte[] octets = address.getAddress();
            int first = octets[0] & 0xFF;
            int second = octets[1] & 0xFF;
            // 0.0.0.0/8
            if (first == 0) {
                return true;
            }
            // 100.64.0.0/10 运营商级 NAT
            if (first == 100 && second >= 64 && second <= 127) {
                return true;
            }
            // 169.254.0.0/16 云厂商元数据
            if (first == 169 && second == 254) {
                return true;
            }
            // 192.0.0.0/24 / 192.0.2.0/24
            if (first == 192 && (second == 0)) {
                return true;
            }
            // 198.18.0.0/15 基准测试
            if (first == 198 && (second == 18 || second == 19)) {
                return true;
            }
            // 240.0.0.0/4 保留
            return first >= 240;
        }
        if (address instanceof Inet6Address) {
            // ::1 已由 isLoopbackAddress 覆盖；fc00::/7 唯一本地地址由 isSiteLocalAddress 不覆盖，额外判
            byte[] bytes = address.getAddress();
            int first = bytes[0] & 0xFF;
            return (first & 0xFE) == 0xFC;
        }
        return false;
    }

    private String stripTrailingSlash(String path) {
        String result = path;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    /**
     * 拼接上游 chat/completions 地址。base_url 已规范化（无尾斜杠）。
     * 若用户填的地址已自带 /chat/completions，直接使用，避免出现双段路径。
     */
    public String chatCompletionsUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "";
        }
        String trimmed = stripTrailingSlash(baseUrl.trim());
        if (trimmed.toLowerCase(Locale.ROOT).endsWith("/chat/completions")) {
            return trimmed;
        }
        return trimmed + "/chat/completions";
    }

    private BusinessException reject(String message) {
        return new BusinessException(ResultCode.PARAM_ERROR, message);
    }
}
