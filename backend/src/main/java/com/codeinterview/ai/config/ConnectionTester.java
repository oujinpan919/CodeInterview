package com.codeinterview.ai.config;

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

import com.codeinterview.ai.AiProperties;
import com.codeinterview.ai.dto.ConnectTestReq;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 连接测试（D4=(c) 分档降级 / 08 §5 第 5 步）。
 *
 * <p>策略：
 * <ol>
 *   <li>先 {@code GET {base_url}/models}——零 token 消耗，顺带拿到模型列表；</li>
 *   <li>仅当返回 <b>404 / 405 / 空数组</b> 时降级为最小 chat 请求（{@code max_tokens=1}）；</li>
 *   <li><b>401 / 403 不降级</b>——凭证错误就是错误，用 chat 掩盖只会让用户更难定位。</li>
 * </ol>
 *
 * <p>结果四态：{@code passed}（/models 通）、{@code passed_degraded}（chat 通）、
 * {@code failed}。测试本身失败不阻塞保存（允许离线保存）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConnectionTester {

    public static final String PASSED = "passed";
    public static final String PASSED_DEGRADED = "passed_degraded";
    public static final String FAILED = "failed";

    private static final int DEFAULT_TIMEOUT_MS = 15_000;
    private static final int MAX_ERROR_BODY = 200;
    private static final int MAX_MODELS = 200;

    private final AiProperties props;
    private final OutboundUrlGuard urlGuard;
    private final ObjectMapper objectMapper;

    /** 测试结果。 */
    public record Result(String statusId,
                         boolean ok,
                         long latencyMs,
                         List<String> models,
                         boolean suggestV1,
                         String error) {
    }

    public Result test(ConnectTestReq req, String resolvedApiKey) {
        String apiKey = resolveKey(req, resolvedApiKey);
        if (apiKey == null || apiKey.isBlank()) {
            return new Result(FAILED, false, 0, List.of(), false, "请填写 API Key");
        }

        OutboundUrlGuard.Validation validation;
        try {
            validation = urlGuard.validate(req.getBaseUrl());
        } catch (RuntimeException ex) {
            return new Result(FAILED, false, 0, List.of(), false, ex.getMessage());
        }

        String baseUrl = validation.normalizedUrl();
        int timeoutMs = req.getTimeoutMs() == null || req.getTimeoutMs() <= 0
                ? DEFAULT_TIMEOUT_MS
                : Math.min(req.getTimeoutMs(), DEFAULT_TIMEOUT_MS);

        long startedAt = System.currentTimeMillis();
        ModelsAttempt modelsAttempt = tryModels(baseUrl, apiKey, timeoutMs);
        long modelsLatency = System.currentTimeMillis() - startedAt;

        if (modelsAttempt.outcome() == Outcome.OK) {
            return new Result(PASSED, true, modelsLatency, modelsAttempt.models(),
                    validation.suggestV1(), null);
        }
        if (modelsAttempt.outcome() == Outcome.AUTH_FAILED) {
            // 401/403：不降级，直接判定凭证问题
            return new Result(FAILED, false, modelsLatency, List.of(), validation.suggestV1(),
                    "凭证无效（HTTP " + modelsAttempt.httpStatus() + "），请检查 API Key");
        }
        if (modelsAttempt.outcome() == Outcome.NETWORK_ERROR) {
            return new Result(FAILED, false, modelsLatency, List.of(), validation.suggestV1(),
                    modelsAttempt.message());
        }

        // 404 / 405 / 空数组 → 降级为最小 chat 请求
        long chatStartedAt = System.currentTimeMillis();
        ChatAttempt chatAttempt = tryChat(baseUrl, apiKey, req.getModelId(), timeoutMs);
        long chatLatency = System.currentTimeMillis() - chatStartedAt;

        if (chatAttempt.ok()) {
            // 该网关没有 /models，模型列表留空由用户手填（D5 兜底）
            return new Result(PASSED_DEGRADED, true, chatLatency, List.of(), validation.suggestV1(), null);
        }
        String message = chatAttempt.message();
        if (chatAttempt.httpStatus() == 404 && validation.suggestV1()) {
            message = message + "（试试在地址后补 /v1）";
        }
        return new Result(FAILED, false, chatLatency, List.of(), validation.suggestV1(), message);
    }

    private String resolveKey(ConnectTestReq req, String resolvedApiKey) {
        if (req.getApiKey() != null && !req.getApiKey().isBlank()) {
            return req.getApiKey().trim();
        }
        return resolvedApiKey;
    }

    // ---- GET /models ----

    private enum Outcome { OK, NOT_SUPPORTED, AUTH_FAILED, NETWORK_ERROR }

    private record ModelsAttempt(Outcome outcome, List<String> models, int httpStatus, String message) {
    }

    private ModelsAttempt tryModels(String baseUrl, String apiKey, int timeoutMs) {
        String url = baseUrl + "/models";
        try {
            HttpResponse<String> response = httpClient(timeoutMs)
                    .send(request(url, apiKey, timeoutMs), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            if (status == 200) {
                List<String> models = parseModels(response.body());
                if (models.isEmpty()) {
                    // 有些网关返回 200 + 空列表，同样视为不支持，走 chat 兜底
                    return new ModelsAttempt(Outcome.NOT_SUPPORTED, List.of(), status, null);
                }
                return new ModelsAttempt(Outcome.OK, models, status, null);
            }
            if (status == 401 || status == 403) {
                return new ModelsAttempt(Outcome.AUTH_FAILED, List.of(), status, null);
            }
            if (status == 404 || status == 405) {
                return new ModelsAttempt(Outcome.NOT_SUPPORTED, List.of(), status, null);
            }
            return new ModelsAttempt(Outcome.NOT_SUPPORTED, List.of(), status, null);
        } catch (HttpTimeoutException ex) {
            return new ModelsAttempt(Outcome.NETWORK_ERROR, List.of(), 0, "连接超时");
        } catch (Exception ex) {
            return new ModelsAttempt(Outcome.NETWORK_ERROR, List.of(), 0, "连接失败：" + safeMessage(ex));
        }
    }

    private List<String> parseModels(String body) {
        List<String> models = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.path("data");
            if (data.isArray()) {
                for (JsonNode node : data) {
                    String id = node.path("id").asText("");
                    if (!id.isBlank()) {
                        models.add(id);
                        if (models.size() >= MAX_MODELS) {
                            break;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("[ai-config] parse /models response failed: {}", ex.getMessage());
        }
        return models;
    }

    // ---- 降级：最小 chat 请求 ----

    private record ChatAttempt(boolean ok, int httpStatus, String message) {
    }

    private ChatAttempt tryChat(String baseUrl, String apiKey, String modelId, int timeoutMs) {
        if (modelId == null || modelId.isBlank()) {
            return new ChatAttempt(false, 0, "该接口不支持 /models，请填写模型 ID 后重试");
        }
        String url = urlGuard.chatCompletionsUrl(baseUrl);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", modelId);
        body.put("stream", false);
        body.put("max_tokens", 1);
        body.put("messages", List.of(Map.of("role", "user", "content", "hi")));
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient(timeoutMs)
                    .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            if (status == 200) {
                return new ChatAttempt(true, status, null);
            }
            String detail = truncate(response.body());
            String message = switch (status) {
                case 401, 403 -> "凭证无效（HTTP " + status + "），请检查 API Key";
                case 404 -> "接口地址不存在（HTTP 404）";
                case 429 -> "上游限流（HTTP 429），稍后可保存重试";
                default -> "上游返回 HTTP " + status + (detail.isBlank() ? "" : "：" + detail);
            };
            return new ChatAttempt(false, status, message);
        } catch (HttpTimeoutException ex) {
            return new ChatAttempt(false, 0, "请求超时");
        } catch (Exception ex) {
            return new ChatAttempt(false, 0, "请求失败：" + safeMessage(ex));
        }
    }

    private HttpRequest request(String url, String apiKey, int timeoutMs) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json")
                .GET()
                .build();
    }

    private HttpClient httpClient(int timeoutMs) {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.min(props.getConnectTimeoutMs(), timeoutMs)))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    private String truncate(String body) {
        if (body == null) {
            return "";
        }
        String single = body.replaceAll("\\s+", " ").trim();
        return single.length() <= MAX_ERROR_BODY ? single : single.substring(0, MAX_ERROR_BODY) + "…";
    }

    /** 异常消息里不得含 Key（JDK 异常本身不含 Authorization 头，这里再兜一次）。 */
    private String safeMessage(Exception ex) {
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        return message.replaceAll("Bearer\\s+\\S+", "Bearer ****");
    }
}
