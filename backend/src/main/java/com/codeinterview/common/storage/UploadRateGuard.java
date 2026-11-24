package com.codeinterview.common.storage;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * 上传频控（每用户每小时）。图片上传是 POST，不在只读 GET 限流拦截器的范围内，
 * 单独用进程内滚动窗口兜底，避免脚本刷 OSS 流量/费用。
 */
@Component
public class UploadRateGuard {

    private static final int LIMIT = 20;
    private static final long WINDOW_MS = 60 * 60 * 1000L;
    private static final int MAX_TRACKED_USERS = 10_000;

    private final Map<Long, Deque<Long>> hits = new ConcurrentHashMap<>();

    public boolean tryAcquire(Long userId) {
        if (userId == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        Deque<Long> deque = hits.computeIfAbsent(userId, key -> new ArrayDeque<>());
        synchronized (deque) {
            while (!deque.isEmpty() && now - deque.peekFirst() >= WINDOW_MS) {
                deque.pollFirst();
            }
            if (deque.size() >= LIMIT) {
                return false;
            }
            deque.addLast(now);
        }
        if (hits.size() > MAX_TRACKED_USERS) {
            hits.clear();
        }
        return true;
    }
}
