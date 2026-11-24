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

import com.codeinterview.ai.config.ModelConfigResolver;
import com.codeinterview.ai.config.OutboundUrlGuard;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 上游流式客户端（05 §2 / §3.1 / 08 §5）：JDK 17 HttpClient + BodyHandlers.ofInputStream()。
 *
 * <p>D1 收敛为<b>纯 OpenAI 兼容</b>后，本类不再有 provider 分支：
 * base_url / model / 凭证 / 参数全部来自 {@link ModelConfigResolver.Resolved}。
 *
 * <p>请求体固定带 {@code "thinking": {"type": "disabled"}}（可由配置覆盖）——讯飞 Spark-X2.6
 * 默认 thinking=enabled，不关会卡首 token 且按输出 token 多计费；同时<b>禁止</b>出现
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

    private final AiProperties props;
    private final ObjectMapper objectMapper;
    private final OutboundUrlGuard urlGuard;

    private volatile HttpClient httpClient;

    /**
     * 建立上游连接并返回 SSE 字节流。
     *
     * @param messages 完整 messages（system + 历史 + 当前）
     * @param config   解析出的生效配置（决定地址、模型、参数、凭证）
     * @param apiKey   解密后的凭证明文（可能是用户自带或站点 Key）
     */
    public InputStream open(List<UpstreamMsg> messages, ModelConfigResolver.Resolved config, String apiKey) {
        HttpRequest request = buildRequest(messages, config, apiKey);
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                HttpResponse<InputStream> response = httpClient(config)
                        .send(request, HttpResponse.BodyHandlers.ofInputStream());
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

    private HttpRequest buildRequest(List<UpstreamMsg> messages,
                                     ModelConfigResolver.Resolved config,
                                     String apiKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.modelId());
        body.put("stream", true);
        // D1：讯飞预设才需要关 thinking；其他网关忽略该字段（透传也安全）
        if (Boolean.TRUE.equals(config.thinkingEnabled()) == false) {
            body.put("thinking", Map.of("type", "disabled"));
        }
        body.put("temperature", config.temperature() == null ? props.getTemperature() : config.temperature());
        body.put("max_tokens", config.maxTokens() == null ? props.getMaxTokens() : config.maxTokens());

        mergeExtraBody(body, config.extraBody());

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

        int timeoutMs = resolveFirstTokenTimeout(config);
        return HttpRequest.newBuilder(URI.create(chatCompletionsUrl(config)))
                // 只约束「等到响应头」：JDK ofInputStream 的正文读取不受该 timeout 限制；
                // 首 token / 空闲 / 总长由 watchdog 负责（05 §2.2）
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .header("Authorization", "Bearer " + (apiKey == null ? "" : apiKey))
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
    }

    /** extra_body 透传：厂商特有字段（如讯飞的自定义参数）合并进请求体顶层。 */
    private void mergeExtraBody(Map<String, Object> body, String extraBody) {
        if (extraBody == null || extraBody.isBlank()) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(extraBody);
            if (!node.isObject()) {
                return;
            }
            node.fields().forEachRemaining(entry -> {
                // 关键字段不允许被 extra_body 覆盖
                if (!List.of("model", "messages", "stream", "max_tokens", "temperature").contains(entry.getKey())) {
                    body.put(entry.getKey(), objectMapper.convertValue(entry.getValue(), Object.class));
                }
            });
        } catch (Exception ex) {
            log.warn("[ai] ignore malformed extra_body: {}", ex.getMessage());
        }
    }

    private int resolveFirstTokenTimeout(ModelConfigResolver.Resolved config) {
        if (config.timeoutMs() != null && config.timeoutMs() > 0) {
            return Math.min(config.timeoutMs(), props.getFirstTokenTimeoutMs());
        }
        return props.getFirstTokenTimeoutMs();
    }

    private String chatCompletionsUrl(ModelConfigResolver.Resolved config) {
        String baseUrl = config.baseUrl() == null ? "" : config.baseUrl().trim();
        // 保存时已校验过一次；实际建连前必须再解析/校验，避免保存后 DNS 改指向内网（rebinding）。
        OutboundUrlGuard.Validation validation = urlGuard.validate(baseUrl);
        return urlGuard.chatCompletionsUrl(validation.normalizedUrl());
    }

    private HttpClient httpClient(ModelConfigResolver.Resolved config) {
        int connectTimeout = props.getConnectTimeoutMs();
        HttpClient client = httpClient;
        if (client == null) {
            synchronized (this) {
                if (httpClient == null) {
                    httpClient = HttpClient.newBuilder()
                            .connectTimeout(Duration.ofMillis(connectTimeout))
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

    /** 上游非 200（首 token 之前）。status 用于 §9.2 映射，不透给前端。 */
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
