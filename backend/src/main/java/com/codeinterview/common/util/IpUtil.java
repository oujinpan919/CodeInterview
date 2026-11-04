package com.codeinterview.common.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端 IP 的唯一取值规则（02 §5.4.1）：
 * 只读 request.getRemoteAddr()，X-Forwarded-* 由 Boot 的 ForwardedHeaderFilter 解析。
 * 禁止手写 XFF 解析（可被伪造，限流形同虚设）。
 */
public final class IpUtil {

    private static final String UNKNOWN = "unknown";

    private IpUtil() {
    }

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        String ip = request.getRemoteAddr();
        if (ip == null || ip.isBlank()) {
            return UNKNOWN;
        }
        // IPv6 映射地址 ::ffff:1.2.3.4 统一取 IPv4 部分
        if (ip.startsWith("::ffff:")) {
            String v4 = ip.substring(7);
            if (!v4.contains(":")) {
                ip = v4;
            }
        }
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }
}
