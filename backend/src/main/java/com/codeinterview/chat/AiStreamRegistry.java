package com.codeinterview.chat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.codeinterview.ai.AiProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 活跃流登记 + 心跳 + watchdog（05 §3.5）。
 *
 * <p>关键约束：
 * <ul>
 *   <li>每个流一把锁，delta 与 ping 共用 —— 防止 emitter 并发写导致事件交错；</li>
 *   <li>watchdog 5s 扫描：20s 首 token / 45s 空闲 / 180s 总长 → 回调 ChatService 发 50202；
 *       距上次写入 ≥15s 发 ping；</li>
 *   <li>客户端断开/超时/取消时关闭上游 InputStream，避免上游继续计费与线程泄漏。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiStreamRegistry {

    private static final long SCAN_INTERVAL_MS = 5000L;
    private static final long PING_INTERVAL_MS = 15000L;

    private final AiProperties props;
    private final ObjectMapper objectMapper;

    private final Map<String, StreamState> streams = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;

    @PostConstruct
    void startWatchdog() {
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "ai-watchdog");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(this::scan, SCAN_INTERVAL_MS, SCAN_INTERVAL_MS, TimeUnit.MILLISECONDS);
        log.info("[ai] watchdog started, scan every {}ms", SCAN_INTERVAL_MS);
    }

    @PreDestroy
    void stopWatchdog() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        streams.values().forEach(this::cancel);
        streams.clear();
    }

    public StreamState register(String messageId, SseEmitter emitter) {
        StreamState state = new StreamState(messageId, emitter);
        streams.put(messageId, state);
        return state;
    }

    public void setTimeoutHandler(StreamState state, Consumer<StreamState> handler) {
        state.timeoutHandler = handler;
    }

    /** 移除流并关闭上游（正常结束、异常结束都走这里，幂等）。 */
    public void unregister(String messageId) {
        StreamState state = streams.remove(messageId);
        if (state != null) {
            cancel(state);
        }
    }

    public void cancel(StreamState state) {
        state.cancelled.set(true);
        closeUpstream(state);
    }

    public void attachUpstream(StreamState state, InputStream upstream) {
        state.upstream = upstream;
    }

    public void markFirstToken(StreamState state, int firstTokenMs) {
        state.firstTokenSent.set(true);
        state.firstTokenMs = firstTokenMs;
    }

    public void touch(StreamState state) {
        state.lastWriteAt = System.currentTimeMillis();
    }

    // ---- 事件发送：全部走同一把锁 ----

    public void sendMeta(StreamState state, String model) {
        send(state, "meta", Map.of("messageId", state.messageId, "model", model));
    }

    public void sendDelta(StreamState state, String content) {
        send(state, "delta", Map.of("content", content));
    }

    public void sendDone(StreamState state, Map<String, Object> payload) {
        send(state, "done", payload);
    }

    public void sendError(StreamState state, int code, String message) {
        send(state, "error", Map.of("code", code, "message", message));
    }

    public void sendErrorQuietly(StreamState state, int code, String message) {
        try {
            sendError(state, code, message);
        } catch (RuntimeException ex) {
            log.debug("[ai] send error event failed (client gone?), messageId={}", state.messageId);
        }
    }

    public void completeQuietly(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (RuntimeException ex) {
            log.debug("[ai] complete emitter failed: {}", ex.getMessage());
        }
    }

    private void sendPing(StreamState state) {
        send(state, "ping", Map.of());
        state.lastPingAt = System.currentTimeMillis();
    }

    private void send(StreamState state, String event, Object data) {
        String json;
        try {
            json = objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("serialize sse payload failed", ex);
        }
        synchronized (state.lock) {
            try {
                state.emitter.send(SseEmitter.event().name(event).data(json, MediaType.TEXT_PLAIN));
                state.lastWriteAt = System.currentTimeMillis();
            } catch (IOException | IllegalStateException ex) {
                state.cancelled.set(true);
                state.clientAborted.set(true);
                closeUpstream(state);
                throw new StreamCancelledException("sse send failed: " + ex.getMessage(), ex);
            }
        }
    }

    private void scan() {
        long now = System.currentTimeMillis();
        for (StreamState state : streams.values()) {
            try {
                if (state.finalized.get()) {
                    continue;
                }
                long total = now - state.startedAt;
                if (total >= props.getTotalTimeoutMs()) {
                    fireTimeout(state, "total timeout");
                    continue;
                }
                if (!state.firstTokenSent.get() && total >= props.getFirstTokenTimeoutMs()) {
                    fireTimeout(state, "first token timeout");
                    continue;
                }
                if (state.firstTokenSent.get() && now - state.lastWriteAt >= props.getIdleTimeoutMs()) {
                    fireTimeout(state, "idle timeout");
                    continue;
                }
                if (now - Math.max(state.lastWriteAt, state.lastPingAt) >= PING_INTERVAL_MS) {
                    sendPing(state);
                }
            } catch (Exception ex) {
                log.warn("[ai] watchdog scan failed, messageId={}, cause={}", state.messageId, ex.getMessage());
            }
        }
    }

    private void fireTimeout(StreamState state, String reason) {
        if (!state.timeoutFired.compareAndSet(false, true)) {
            return;
        }
        state.cancelled.set(true);
        closeUpstream(state);
        Consumer<StreamState> handler = state.timeoutHandler;
        if (handler == null) {
            return;
        }
        try {
            handler.accept(state);
        } catch (Exception ex) {
            log.warn("[ai] timeout handler failed, messageId={}, reason={}, cause={}",
                    state.messageId, reason, ex.getMessage());
        }
    }

    private void closeUpstream(StreamState state) {
        InputStream upstream = state.upstream;
        state.upstream = null;
        if (upstream != null) {
            try {
                upstream.close();
            } catch (IOException ex) {
                log.debug("[ai] close upstream failed: {}", ex.getMessage());
            }
        }
    }

    /** 每个活跃流的可变状态；finalized/timeoutFired 保证收尾与超时只执行一次。 */
    public static final class StreamState {

        public final String messageId;
        public final SseEmitter emitter;
        public final Object lock = new Object();
        public final long startedAt = System.currentTimeMillis();
        public final AtomicBoolean firstTokenSent = new AtomicBoolean(false);
        public final AtomicBoolean cancelled = new AtomicBoolean(false);
        public final AtomicBoolean clientAborted = new AtomicBoolean(false);
        public final AtomicBoolean finalized = new AtomicBoolean(false);
        public final AtomicBoolean timeoutFired = new AtomicBoolean(false);
        public volatile long lastWriteAt = System.currentTimeMillis();
        public volatile long lastPingAt;
        public volatile InputStream upstream;
        public volatile Integer firstTokenMs;
        public volatile Consumer<StreamState> timeoutHandler;

        StreamState(String messageId, SseEmitter emitter) {
            this.messageId = messageId;
            this.emitter = emitter;
        }
    }

    /** 发送事件时客户端已断开（不能当作上游失败）。 */
    public static class StreamCancelledException extends RuntimeException {

        public StreamCancelledException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
