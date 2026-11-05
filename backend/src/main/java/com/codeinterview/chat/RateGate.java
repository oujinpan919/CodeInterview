package com.codeinterview.chat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Component;

import com.codeinterview.ai.AiProperties;

import lombok.RequiredArgsConstructor;

/**
 * 助手闸门（05 §3.2 step 3 / §5.4）：同账号 3s 间隔 → 账号并发 → 全站并发。
 * 单机内存实现，不做分布式限流（05 §1 边界）。
 */
@Component
@RequiredArgsConstructor
public class RateGate {

    /** 闸门结果：调用方据此映射 42901 / 42902。 */
    public enum GateResult {
        ACQUIRED,
        USER_LIMIT,
        SITE_LIMIT
    }

    private final AiProperties props;

    private final Map<Long, Long> lastRequestAt = new ConcurrentHashMap<>();
    private final Map<Long, Semaphore> userSemaphores = new ConcurrentHashMap<>();
    private final AtomicBoolean siteSemaphoreReady = new AtomicBoolean(false);
    private volatile Semaphore siteSemaphore;

    public GateResult tryAcquire(Long userId) {
        long now = System.currentTimeMillis();
        if (!acquireInterval(userId, now)) {
            return GateResult.USER_LIMIT;
        }
        Semaphore userSemaphore = userSemaphores.computeIfAbsent(userId,
                key -> new Semaphore(props.getLimit().getUserConcurrency()));
        if (!userSemaphore.tryAcquire()) {
            return GateResult.USER_LIMIT;
        }
        if (!siteSemaphore().tryAcquire()) {
            userSemaphore.release();
            return GateResult.SITE_LIMIT;
        }
        return GateResult.ACQUIRED;
    }

    public void release(Long userId) {
        Semaphore userSemaphore = userSemaphores.get(userId);
        if (userSemaphore != null) {
            userSemaphore.release();
        }
        Semaphore site = siteSemaphore;
        if (site != null) {
            site.release();
        }
    }

    /** 同账号最小间隔：原子地把上次请求时间推进到现在，返回是否放行。 */
    private boolean acquireInterval(Long userId, long now) {
        AtomicBoolean allowed = new AtomicBoolean(false);
        lastRequestAt.compute(userId, (key, previous) -> {
            if (previous == null || now - previous >= props.getLimit().getMinIntervalMs()) {
                allowed.set(true);
                return now;
            }
            return previous;
        });
        return allowed.get();
    }

    private Semaphore siteSemaphore() {
        if (!siteSemaphoreReady.get()) {
            synchronized (this) {
                if (siteSemaphore == null) {
                    siteSemaphore = new Semaphore(props.getLimit().getSiteConcurrency());
                    siteSemaphoreReady.set(true);
                }
            }
        }
        return siteSemaphore;
    }

    /** 仅测试用：清空间隔与信号量状态，避免用例互相影响。 */
    public void resetForTest() {
        lastRequestAt.clear();
        userSemaphores.clear();
        siteSemaphoreReady.set(false);
        siteSemaphore = null;
    }
}
