package com.codeinterview.auth;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * 登录失败锁定与注册限频（02 §5.4.1），进程内计数，单实例假设。
 * 失败键为 IP + 昵称：10 分钟内 5 次失败 → 锁 10 分钟；成功登录清零。
 * 注册限频：同 IP 每小时不超过 5 次。
 */
@Component
public class LoginAttemptGuard {

    private static final int MAX_FAILURES = 5;
    private static final long WINDOW_MS = 10 * 60 * 1000L;
    private static final long LOCK_MS = 10 * 60 * 1000L;
    private static final int REGISTER_LIMIT = 5;
    private static final long REGISTER_WINDOW_MS = 60 * 60 * 1000L;

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> registerHits = new ConcurrentHashMap<>();

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
        Attempt attempt = attempts.computeIfAbsent(key(ip, nickname), k -> new Attempt());
        synchronized (attempt) {
            long now = System.currentTimeMillis();
            while (!attempt.failures.isEmpty() && now - attempt.failures.peekFirst() > WINDOW_MS) {
                attempt.failures.pollFirst();
            }
            attempt.failures.addLast(now);
            if (attempt.failures.size() >= MAX_FAILURES) {
                attempt.lockedUntil = now + LOCK_MS;
                attempt.failures.clear();
            }
        }
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
            return true;
        }
    }

    private String key(String ip, String nickname) {
        return (ip == null ? "" : ip) + "|" + (nickname == null ? "" : nickname);
    }

    private static final class Attempt {
        private final Deque<Long> failures = new ArrayDeque<>();
        private long lockedUntil = 0L;
    }
}
