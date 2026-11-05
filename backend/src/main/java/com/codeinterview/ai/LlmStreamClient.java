package com.codeinterview.ai;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 上游流式客户端（05 §2 / §3.1）：JDK 17 HttpClient + BodyHandlers.ofInputStream()。
 *
 * <p>请求体固定带 {@code "thinking": {"type": "disabled"}} —— 讯飞 Spark-X2.6 默认
 * thinking=enabled，不关会卡首 token 且按输出 token 多计费；同时<b>禁止</b>出现
 * stream_options（讯飞不认，默认就在末个 chunk 带 usage）。
 *
 * <p>重试口径（05 §2.2）：仅在首 token 之前、最多 1 次，只针对 IOException 与上游 5xx；
 * 401/400/429、HttpTimeoutException、code != 0 均不重试。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmStreamClient {

    private static final int MAX_ATTEMPTS = 2;
    private static final int MAX_ERROR_BODY_CHARS = 512;
    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    private final AiProperties props;
    private final ObjectMapper objectMapper;

    private volatile HttpClient httpClient;

    /**
     * 建立上游连接并返回 SSE 字节流。HTTP 状态非 200 时在这里失败（此时还没读到任何 delta，
     * 属于「首 token 之前」）；HTTP 200 + body.code != 0 由 {@link UpstreamSseParser} 逐行暴露。
     */
    public InputStream open(List<UpstreamMsg> messages) {
        HttpRequest request = buildRequest(messages);
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                HttpResponse<InputStream> response = httpClient().send(request, HttpResponse.BodyHandlers.ofInputStream());
                int status = response.statusCode();
                if (status == 200) {
                    if (attempt > 1) {
                        log.info("[ai] upstream recovered after retry, attempts={}", attempt);
                    }
                    return response.body();
                }
                String errorBody = readErrorBody(response.body());
                if (status >= 500 && attempt < MAX_ATTEMPTS) {
                    log.warn("[ai] upstream http status={} before first token, retry once", status);
                    continue;
                }
                log.warn("[ai] upstream http status={}, errorBody={}", status, errorBody);
                throw new UpstreamHttpException(status);
            } catch (HttpTimeoutException ex) {
                // 首 token 超时不做自动重试，避免用户等 40s（05 §2.2）
                throw new UpstreamTimeoutException("等待上游响应超时", ex);
            } catch (IOException ex) {
                if (attempt < MAX_ATTEMPTS) {
                    log.warn("[ai] upstream connect failed before first token, retry once: {}", ex.getMessage());
                    continue;
                }
                throw new UpstreamConnectException("上游连接失败", ex);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new UpstreamConnectException("上游连接被中断", ex);
            }
        }
    }

    private HttpRequest buildRequest(List<UpstreamMsg> messages) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", props.getModel());
        body.put("stream", true);
        // 讯飞 Spark-X2.6 默认 thinking=enabled：必须显式关闭（05 §2.1）
        body.put("thinking", Map.of("type", "disabled"));
        body.put("temperature", props.getTemperature());
        body.put("max_tokens", props.getMaxTokens());

        List<Map<String, String>> payloadMessages = new ArrayList<>(messages == null ? 0 : messages.size());
        if (messages != null) {
            for (UpstreamMsg message : messages) {
                payloadMessages.add(Map.of("role", message.role(), "content", message.content()));
            }
        }
        body.put("messages", payloadMessages);

        String json;
        try {
            json = objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("serialize upstream request failed", ex);
        }
        String apiKey = props.getApiKey() == null ? "" : props.getApiKey();
        return HttpRequest.newBuilder(URI.create(chatCompletionsUrl()))
                // 只约束「等到响应头」：JDK ofInputStream 的正文读取不受该 timeout 限制（实测 3s 超时下
                // 8s 流式响应仍能读完）；首 token 20s / 空闲 45s / 总长 180s 由 watchdog 负责（05 §2.2）
                .timeout(Duration.ofMillis(props.getFirstTokenTimeoutMs()))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
    }

    private String chatCompletionsUrl() {
        String baseUrl = props.getBaseUrl() == null ? "" : props.getBaseUrl().trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + CHAT_COMPLETIONS_PATH;
    }

    private HttpClient httpClient() {
        HttpClient client = httpClient;
        if (client == null) {
            synchronized (this) {
                if (httpClient == null) {
                    httpClient = HttpClient.newBuilder()
                            .connectTimeout(Duration.ofMillis(props.getConnectTimeoutMs()))
                            .version(HttpClient.Version.HTTP_1_1)
                            .build();
                }
                client = httpClient;
            }
        }
        return client;
    }

    /** 读错误响应体（限长，仅用于日志/异常信息；不返回给前端）。 */
    private String readErrorBody(InputStream stream) {
        if (stream == null) {
            return "";
        }
        try (InputStream in = stream) {
            byte[] buffer = new byte[MAX_ERROR_BODY_CHARS];
            int read = in.read(buffer);
            if (read <= 0) {
                return "";
            }
            return new String(buffer, 0, read, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "";
        }
    }

    /** 上游非 200（首 token 之前）。status 用于 §9.2 映射，不透给前端；错误体已在 WARN 日志里。 */
    public static class UpstreamHttpException extends RuntimeException {

        private final int status;

        public UpstreamHttpException(int status) {
            super("upstream http " + status);
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }

    /** 等待响应头超时（首 token 超时）。 */
    public static class UpstreamTimeoutException extends RuntimeException {

        public UpstreamTimeoutException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /** 连接失败/被重置，重试后仍失败。 */
    public static class UpstreamConnectException extends RuntimeException {

        public UpstreamConnectException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
