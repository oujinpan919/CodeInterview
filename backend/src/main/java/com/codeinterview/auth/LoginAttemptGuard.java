package com.codeinterview.auth;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 登录失败锁定与注册限频（02 §5.4.1），进程内计数，单实例假设。
 * 失败键为 IP + 昵称：10 分钟内 5 次失败 → 锁 10 分钟；成功登录清零。
 * 注册限频：同 IP 每小时不超过 5 次。
 *
 * <p>T17 加固：每 256 次操作清理一次过期条目，并对 Map 大小做防御性上限，
 * 避免用随机昵称/IP 把进程内计数打爆内存（单机一人使用，清空只损失短时锁定）。
 */
@Slf4j
@Component
public class LoginAttemptGuard {

    private static final int MAX_FAILURES = 5;
    private static final long WINDOW_MS = 10 * 60 * 1000L;
    private static final long LOCK_MS = 10 * 60 * 1000L;
    private static final int REGISTER_LIMIT = 5;
    private static final long REGISTER_WINDOW_MS = 60 * 60 * 1000L;
    private static final int SWEEP_EVERY_OPS = 256;
    private static final int MAX_TRACKED_KEYS = 20_000;

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> registerHits = new ConcurrentHashMap<>();
    private final AtomicLong operations = new AtomicLong();

    public boolean isLocked(String ip, String nickname) {
        Attempt attempt = attempts.get(key(ip, nickname));
        if (attempt == null) {
            return false;
        }
        synchronized (attempt) {
            long now = System.currentTimeMillis();
            if (attempt.lockedUntil > now) {
                return true;
            }
            if (attempt.lockedUntil > 0) {
                attempt.lockedUntil = 0L;
                attempt.failures.clear();
            }
            return false;
        }
    }

    public void recordFailure(String ip, String nickname) {
        long now = System.currentTimeMillis();
        Attempt attempt = attempts.computeIfAbsent(key(ip, nickname), k -> new Attempt());
        synchronized (attempt) {
            while (!attempt.failures.isEmpty() && now - attempt.failures.peekFirst() > WINDOW_MS) {
                attempt.failures.pollFirst();
            }
            attempt.failures.addLast(now);
            if (attempt.failures.size() >= MAX_FAILURES) {
                attempt.lockedUntil = now + LOCK_MS;
                attempt.failures.clear();
            }
        }
        maybeSweep(now);
    }

    public void clear(String ip, String nickname) {
        attempts.remove(key(ip, nickname));
    }

    /**
     * @return true 表示本次注册放行，false 表示超过同 IP 每小时 5 次限制
     */
    public boolean tryRegister(String ip) {
        Deque<Long> hits = registerHits.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (hits) {
            long now = System.currentTimeMillis();
            while (!hits.isEmpty() && now - hits.peekFirst() > REGISTER_WINDOW_MS) {
                hits.pollFirst();
            }
            if (hits.size() >= REGISTER_LIMIT) {
                return false;
            }
            hits.addLast(now);
            maybeSweep(now);
            return true;
        }
    }

    /** 周期清理：过期失败记录、过期注册命中；防御性上限避免内存被随机 key 打爆。 */
    private void maybeSweep(long now) {
        if (operations.incrementAndGet() % SWEEP_EVERY_OPS != 0) {
            return;
        }
        attempts.entrySet().removeIf(entry -> {
            Attempt attempt = entry.getValue();
            synchronized (attempt) {
                while (!attempt.failures.isEmpty() && now - attempt.failures.peekFirst() > WINDOW_MS) {
                    attempt.failures.pollFirst();
                }
                return attempt.lockedUntil <= now && attempt.failures.isEmpty();
            }
        });
        registerHits.entrySet().removeIf(entry -> {
            Deque<Long> deque = entry.getValue();
            synchronized (deque) {
                while (!deque.isEmpty() && now - deque.peekFirst() > REGISTER_WINDOW_MS) {
                    deque.pollFirst();
                }
                return deque.isEmpty();
            }
        });
        if (attempts.size() > MAX_TRACKED_KEYS) {
            log.warn("login attempt map exceeded {}, clear all entries", MAX_TRACKED_KEYS);
            attempts.clear();
        }
    }

    private String key(String ip, String nickname) {
        // 昵称唯一键大小写不敏感（uk_nickname），锁定键必须同样归一，否则 Tom/tom 交替可绕过 10 分钟 5 次
        String normalizedNickname = nickname == null ? "" : nickname.trim().toLowerCase(java.util.Locale.ROOT);
        return (ip == null ? "" : ip) + "|" + normalizedNickname;
    }

    private static final class Attempt {
        private final Deque<Long> failures = new ArrayDeque<>();
        private long lockedUntil = 0L;
    }
}
