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

    /**
     * 闸门结果：调用方据此映射业务码。
     *
     * <p><b>v3.20 修复</b>：原实现把「3s 间隔未到」「账号已有流在跑」与「账号额度用尽」
     * 三种完全不同的原因共用 {@code USER_LIMIT}，导致 {@code ChatService} 全部映射成
     * 42901「今日额度已用完，请明天再试」——用户看到「明天再试」就不会再试了。
     * 现在按原因拆开：前两者是「稍等即可」（42900），后者才是真额度用尽（42901）。
     */
    public enum GateResult {
        ACQUIRED,
        /** 同账号两次请求间隔 < min-interval-ms。 */
        TOO_FREQUENT,
        /** 同账号已有进行中的流（并发=1）。 */
        CONCURRENT,
        /** 全站并发已满。 */
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
            return GateResult.TOO_FREQUENT;
        }
        Semaphore userSemaphore = userSemaphores.computeIfAbsent(userId,
                key -> new Semaphore(props.getLimit().getUserConcurrency()));
        if (!userSemaphore.tryAcquire()) {
            return GateResult.CONCURRENT;
        }
        if (!siteSemaphore().tryAcquire()) {
            userSemaphore.release();
            return GateResult.SITE_LIMIT;
        }
        return GateResult.ACQUIRED;
    }

    /** 间隔闸门的剩余等待毫秒数（>0 表示还需等待），供错误文案精确提示。 */
    public long remainingIntervalMs(Long userId) {
        Long previous = lastRequestAt.get(userId);
        if (previous == null) {
            return 0L;
        }
        long elapsed = System.currentTimeMillis() - previous;
        return Math.max(0L, props.getLimit().getMinIntervalMs() - elapsed);
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
