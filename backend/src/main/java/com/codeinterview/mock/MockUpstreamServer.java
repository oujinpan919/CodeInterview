package com.codeinterview.mock;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import lombok.extern.slf4j.Slf4j;

/**
 * Mock 上游（05 §10）：{@code @Profile("mock")} 下启动 JDK HttpServer 监听 127.0.0.1:18080，
 * 按请求体里的 model 切剧本，输出与讯飞真实响应结构一致的 SSE（每行都带 code: 0）。
 *
 * <p>剧本：mock-ok / mock-slow / mock-break / mock-429 / mock-500 / mock-nousage（05 §10.3），
 * 另有两个用于验收的上游错误剧本：mock-code（HTTP 200 + body.code=11201）、
 * mock-security（security_suggest.action=HIDE_CONTINUE 片段）。
 *
 * <p>红线：业务代码不得 new 本类；本类不会进入 prod 启动路径（@Profile 保证）。
 */
@Slf4j
@Component
@Profile("mock")
public class MockUpstreamServer implements SmartLifecycle {

    public static final int DEFAULT_PORT = 18080;
    public static final String PATH = "/chat/completions";

    private static final int DELTA_COUNT = 20;
    private static final long DELTA_INTERVAL_MS = 120L;
    private static final long SLOW_FIRST_DELAY_MS = 25_000L;
    private static final String SSE_CONTENT_TYPE = "text/event-stream; charset=utf-8";
    private static final String HIDDEN_CONTENT = "这段不应该被转发";

    /** 20 段增量拼成一段带 Java 代码块的 Markdown 回答（前端定稿后代码高亮可验证）。 */
    private static final String[] ANSWER_CHUNKS = {
        "HashMap 的扩容机制可以概括为三点：\n\n",
        "1. 默认容量 16，负载因子 0.75，",
        "阈值 = 容量 × 负载因子。\n",
        "2. 当元素数量超过阈值时，",
        "容量翻倍为原来的 2 倍。\n",
        "3. 扩容后需要重新计算每个桶的位置，",
        "JDK 8 用高位与运算避免重复哈希。\n\n",
        "核心代码大致如下：\n\n",
        "```java\n",
        "Node<K,V>[] resize() {\n",
        "    Node<K,V>[] oldTab = table;\n",
        "    int oldCap = (oldTab == null) ? 0 : oldTab.length;\n",
        "    int newCap = oldCap << 1;\n",
        "    Node<K,V>[] newTab = new Node[newCap];\n",
        "    table = newTab;\n",
        "    return newTab;\n",
        "}\n",
        "```\n\n",
        "结论：扩容本质是「翻倍 + 迁移」，",
        "代价是 O(n) 的重新散列，所以初始化时给合理容量能减少扩容次数。"
    };

    private final ObjectMapper objectMapper;
    private final int port;

    private final Map<String, AtomicInteger> modelHits = new ConcurrentHashMap<>();
    private final AtomicInteger totalRequests = new AtomicInteger();
    private final AtomicBoolean mock500First = new AtomicBoolean(true);
    private volatile String lastRequestBody = "";

    private volatile HttpServer server;
    private volatile ExecutorService executor;

    @Autowired
    public MockUpstreamServer(ObjectMapper objectMapper) {
        this(objectMapper, DEFAULT_PORT);
    }

    public MockUpstreamServer(ObjectMapper objectMapper, int port) {
        this.objectMapper = objectMapper;
        this.port = port;
    }

    @Override
    public synchronized void start() {
        if (server != null) {
            return;
        }
        try {
            HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            httpServer.createContext(PATH, this::handle);
            executor = Executors.newFixedThreadPool(8, runnable -> {
                Thread thread = new Thread(runnable, "mock-upstream-");
                thread.setDaemon(true);
                return thread;
            });
            httpServer.setExecutor(executor);
            httpServer.start();
            server = httpServer;
            log.info("[mock] upstream started: http://127.0.0.1:{}{}", port, PATH);
        } catch (IOException ex) {
            throw new IllegalStateException("mock upstream start failed on port " + port, ex);
        }
    }

    @Override
    public synchronized void stop() {
        HttpServer current = server;
        server = null;
        if (current != null) {
            current.stop(0);
        }
        ExecutorService pool = executor;
        executor = null;
        if (pool != null) {
            pool.shutdownNow();
        }
        log.info("[mock] upstream stopped");
    }

    @Override
    public boolean isRunning() {
        return server != null;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return 0;
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            if (!PATH.equals(exchange.getRequestURI().getPath()) || !"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 404, "{\"code\":40400,\"message\":\"not found\"}");
                return;
            }
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            lastRequestBody = body;
            totalRequests.incrementAndGet();
            String model = readModel(body);
            modelHits.computeIfAbsent(model, key -> new AtomicInteger()).incrementAndGet();
            log.info("[mock] request #{} model={}", totalRequests.get(), model);

            switch (model) {
                case "mock-429" -> sendJson(exchange, 429, "{\"code\":11202,\"message\":\"rate limited\"}");
                case "mock-500" -> {
                    if (mock500First.getAndSet(false)) {
                        sendJson(exchange, 500, "{\"code\":50000,\"message\":\"upstream error\"}");
                    } else {
                        streamAnswer(exchange, false, false);
                    }
                }
                case "mock-slow" -> {
                    sleep(SLOW_FIRST_DELAY_MS);
                    streamAnswer(exchange, false, false);
                }
                case "mock-break" -> streamBroken(exchange);
                case "mock-nousage" -> streamAnswer(exchange, true, false);
                case "mock-security" -> streamAnswer(exchange, false, true);
                case "mock-code" -> streamCodeError(exchange);
                default -> streamAnswer(exchange, false, false);
            }
        } catch (IOException ex) {
            log.debug("[mock] client closed before response completed: {}", ex.getMessage());
        } catch (RuntimeException ex) {
            log.warn("[mock] handler error: {}", ex.getMessage());
        } finally {
            exchange.close();
        }
    }

    private void streamAnswer(HttpExchange exchange, boolean noUsage, boolean withHiddenSegment) throws IOException {
        prepareSse(exchange);
        OutputStream out = exchange.getResponseBody();
        try {
            if (withHiddenSegment) {
                writeSse(out, sseDelta(HIDDEN_CONTENT, true));
            }
            for (int i = 0; i < DELTA_COUNT; i++) {
                writeSse(out, sseDelta(ANSWER_CHUNKS[i], false));
                sleep(DELTA_INTERVAL_MS);
            }
            writeSse(out, sseFinal(noUsage));
            writeRaw(out, "data: [DONE]\n\n");
        } finally {
            closeQuietly(out);
        }
    }

    private void streamBroken(HttpExchange exchange) throws IOException {
        prepareSse(exchange);
        OutputStream out = exchange.getResponseBody();
        try {
            // 发 3 个 delta 后直接 EOF，不给 [DONE]：模拟上游中途断流
            for (int i = 0; i < 3; i++) {
                writeSse(out, sseDelta(ANSWER_CHUNKS[i], false));
                sleep(DELTA_INTERVAL_MS);
            }
        } finally {
            closeQuietly(out);
        }
    }

    private void streamCodeError(HttpExchange exchange) throws IOException {
        prepareSse(exchange);
        OutputStream out = exchange.getResponseBody();
        try {
            Map<String, Object> root = baseLine();
            root.put("code", 11201);
            root.put("message", "day quota exceeded");
            writeSse(out, root);
            writeRaw(out, "data: [DONE]\n\n");
        } finally {
            closeQuietly(out);
        }
    }

    private void prepareSse(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", SSE_CONTENT_TYPE);
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.getResponseHeaders().set("X-Accel-Buffering", "no");
        exchange.sendResponseHeaders(200, 0);
    }

    private void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
            out.flush();
        }
    }

    private Map<String, Object> baseLine() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("code", 0);
        root.put("message", "Success");
        root.put("sid", "mock0001");
        root.put("id", "mock");
        root.put("created", System.currentTimeMillis() / 1000);
        return root;
    }

    private String sseDelta(String content, boolean hideContinue) throws IOException {
        Map<String, Object> root = baseLine();
        Map<String, Object> delta = new LinkedHashMap<>();
        delta.put("content", content);
        if (hideContinue) {
            delta.put("security_suggest", Map.of("action", "HIDE_CONTINUE"));
        }
        Map<String, Object> choice = new LinkedHashMap<>();
        choice.put("delta", delta);
        choice.put("index", 0);
        root.put("choices", List.of(choice));
        return objectMapper.writeValueAsString(root);
    }

    private String sseFinal(boolean noUsage) throws IOException {
        Map<String, Object> root = baseLine();
        Map<String, Object> choice = new LinkedHashMap<>();
        choice.put("delta", Map.of());
        choice.put("finish_reason", "stop");
        choice.put("index", 0);
        root.put("choices", List.of(choice));
        if (!noUsage) {
            Map<String, Object> usage = new LinkedHashMap<>();
            usage.put("prompt_tokens", 12);
            usage.put("completion_tokens", 20);
            usage.put("total_tokens", 32);
            root.put("usage", usage);
        }
        return objectMapper.writeValueAsString(root);
    }

    private void writeSse(OutputStream out, String payload) throws IOException {
        writeRaw(out, "data: " + payload + "\n\n");
    }

    private void writeSse(OutputStream out, Map<String, Object> payload) throws IOException {
        writeSse(out, objectMapper.writeValueAsString(payload));
    }

    private void writeRaw(OutputStream out, String text) throws IOException {
        out.write(text.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private String readModel(String body) {
        if (body == null || body.isBlank()) {
            return "mock-ok";
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            String model = root.path("model").asText("");
            return model.isBlank() ? "mock-ok" : model;
        } catch (IOException ex) {
            return "mock-ok";
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void closeQuietly(OutputStream out) {
        try {
            out.close();
        } catch (IOException ignored) {
            // 客户端已断开时关闭会抛异常，忽略
        }
    }

    // ---- 供单测断言用 ----

    public int hits(String model) {
        AtomicInteger hit = modelHits.get(model);
        return hit == null ? 0 : hit.get();
    }

    public int totalRequests() {
        return totalRequests.get();
    }

    public String lastRequestBody() {
        return lastRequestBody;
    }

    public void resetCounters() {
        modelHits.clear();
        totalRequests.set(0);
        mock500First.set(true);
        lastRequestBody = "";
    }
}
