package com.codeinterview.common.interceptor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.codeinterview.common.exception.BusinessException;
import com.codeinterview.common.result.ResultCode;
import com.codeinterview.common.util.IpUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 普通读接口限流（01 F11 / 06 T17）：进程内按 IP 的滚动窗口，只对只读 GET 生效，超限 42900。
 *
 * <p>三条约束：
 * <ul>
 *   <li>只读 GET：写操作、登录/注册、AI 对话（POST）不套这个桶；</li>
 *   <li>拦截层自身异常必须放行（不允许因为限流组件故障把站打挂）；</li>
 *   <li>不加锁排队：每个 IP 一把对象锁，未命中直接拒绝，不阻塞请求线程。</li>
 * </ul>
 *
 * <p>阈值：06 T17 验收口径 60 次/分钟（01 F11/02 §5.4.1 的 120 次与本卡验收冲突，
 * 按更严格的验收口径实现，已登记 02 §10）。
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final long WINDOW_MS = 60_000L;
    private static final int SWEEP_EVERY_OPS = 500;
    private static final int MAX_TRACKED_IPS = 10_000;

    private final int limit;

    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();
    private final AtomicLong operations = new AtomicLong();

    public RateLimitInterceptor(@Value("${app.rate-limit.read-per-minute:60}") int limit) {
        this.limit = Math.max(1, limit);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (!path.startsWith("/api/v1/")) {
            return true;
        }
        boolean allowed;
        try {
            allowed = tryAcquire(IpUtil.getClientIp(request));
        } catch (RuntimeException ex) {
            // 限流组件异常时放行（02 §5.6 / 01 F11 硬要求）
            log.warn("[rate-limit] interceptor failed, allow request: path={} cause={}", path, ex.getMessage());
            return true;
        }
        if (!allowed) {
            throw new BusinessException(ResultCode.RATE_LIMITED, "操作太频繁，请稍后再试");
        }
        return true;
    }

    private boolean tryAcquire(String ip) {
        long now = System.currentTimeMillis();
        maybeSweep(now);
        Deque<Long> deque = hits.computeIfAbsent(ip, key -> new ArrayDeque<>());
        synchronized (deque) {
            while (!deque.isEmpty() && now - deque.peekFirst() >= WINDOW_MS) {
                deque.pollFirst();
            }
            if (deque.size() >= limit) {
                return false;
            }
            deque.addLast(now);
            return true;
        }
    }

    private void maybeSweep(long now) {
        if (operations.incrementAndGet() % SWEEP_EVERY_OPS != 0) {
            return;
        }
        hits.entrySet().removeIf(entry -> {
            Deque<Long> deque = entry.getValue();
            synchronized (deque) {
                while (!deque.isEmpty() && now - deque.peekFirst() >= WINDOW_MS) {
                    deque.pollFirst();
                }
                return deque.isEmpty();
            }
        });
        if (hits.size() > MAX_TRACKED_IPS) {
            // 防御性上限：异常流量下也不让进程内 Map 无限增长（单机一人使用）
            log.warn("[rate-limit] tracked ips exceeded {}, clear all windows", MAX_TRACKED_IPS);
            hits.clear();
        }
    }

    /** 仅测试用：清空窗口计数。 */
    public void resetForTest() {
        hits.clear();
        operations.set(0);
    }

    public int getLimit() {
        return limit;
    }
}
