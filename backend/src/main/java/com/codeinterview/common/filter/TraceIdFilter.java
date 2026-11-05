package com.codeinterview.common.filter;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * traceId 过滤器（02 §5.6 / 06 T17）：
 * 每个请求生成 8 位 traceId 写进 MDC（logback 的 %X{traceId:-} 会输出），
 * 响应头回写 X-Trace-Id 便于前端/排查对齐；请求结束打印一条
 * {@code [traceId] method path userId costMs status}，并清理 MDC（线程池复用不串号）。
 *
 * <p>userId 用 {@code StpUtil.getLoginIdByToken(token)} 从请求头 token 直接解出：
 * Filter 结束阶段不在 Sa-Token 的请求上下文里，不能调用需要上下文的 StpUtil 方法。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "traceId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Value("${sa-token.token-name:satoken}")
    private String tokenName;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        MDC.put(TRACE_ID, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        long startedAt = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long costMs = System.currentTimeMillis() - startedAt;
            Long userId = currentUserIdOrNull(request);
            log.info("{} {} userId={} costMs={} status={}",
                    request.getMethod(), request.getRequestURI(),
                    userId == null ? "-" : userId, costMs, response.getStatus());
            MDC.remove(TRACE_ID);
        }
    }

    private Long currentUserIdOrNull(HttpServletRequest request) {
        try {
            String token = request.getHeader(tokenName);
            if (token == null || token.isBlank()) {
                return null;
            }
            Object loginId = StpUtil.getLoginIdByToken(token);
            return loginId == null ? null : Long.valueOf(String.valueOf(loginId));
        } catch (Exception ex) {
            // token 无效/已过期：日志里记 “-”，不影响请求本身
            return null;
        }
    }
}
