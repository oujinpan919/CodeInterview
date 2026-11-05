package com.codeinterview.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.codeinterview.ai.AiExecutorConfig;
import com.codeinterview.ai.AiProperties;
import com.codeinterview.ai.LlmStreamClient;
import com.codeinterview.ai.PromptBuilder;
import com.codeinterview.ai.UpstreamCodeMapper;
import com.codeinterview.ai.UpstreamMsg;
import com.codeinterview.ai.UpstreamSseParser;
import com.codeinterview.chat.dto.ChatReq;
import com.codeinterview.chat.dto.Msg;
import com.codeinterview.chat.entity.AiChatLog;
import com.codeinterview.common.exception.BusinessException;
import com.codeinterview.common.result.ResultCode;

import lombok.extern.slf4j.Slf4j;

/**
 * 助手编排（05 §3.2 严格按序）：校验 → 开关 → 闸门 → 预扣 → 建流 → 提交线程池 → 流式转发 → 收尾计费。
 *
 * <p>硬约束：
 * <ul>
 *   <li>userId 由 Controller 传入，worker 线程内<b>禁止</b>调用 StpUtil；</li>
 *   <li>失败点与回滚严格对应 05 §5.3；只有「首 token 之前失败」回滚两项配额；</li>
 *   <li>meta 是第一个事件、done 是正常结束的最后一个事件；流开始后异常只发 event:error；</li>
 *   <li>每个流一把锁（delta 与 ping 共用，锁在 AiStreamRegistry 内）。</li>
 * </ul>
 */
@Slf4j
@Service
public class ChatService {

    private static final long EMITTER_TIMEOUT_MS = 190_000L;
    private static final double QUOTA_WARN_RATIO = 0.8;
    private static final int UPSTREAM_FAILURE_ALERT_THRESHOLD = 5;

    private final ChatValidator validator;
    private final PromptBuilder promptBuilder;
    private final LlmStreamClient llmStreamClient;
    private final UpstreamSseParser parser;
    private final UpstreamCodeMapper upstreamCodeMapper;
    private final QuotaService quotaService;
    private final RateGate rateGate;
    private final AiStreamRegistry registry;
    private final AiChatLogService chatLogService;
    private final AiProperties props;
    private final ThreadPoolTaskExecutor aiExecutor;
    private final AtomicInteger consecutiveUpstreamFailures = new AtomicInteger();

    public ChatService(ChatValidator validator,
                       PromptBuilder promptBuilder,
                       LlmStreamClient llmStreamClient,
                       UpstreamSseParser parser,
                       UpstreamCodeMapper upstreamCodeMapper,
                       QuotaService quotaService,
                       RateGate rateGate,
                       AiStreamRegistry registry,
                       AiChatLogService chatLogService,
                       AiProperties props,
                       @Qualifier(AiExecutorConfig.AI_EXECUTOR) ThreadPoolTaskExecutor aiExecutor) {
        this.validator = validator;
        this.promptBuilder = promptBuilder;
        this.llmStreamClient = llmStreamClient;
        this.parser = parser;
        this.upstreamCodeMapper = upstreamCodeMapper;
        this.quotaService = quotaService;
        this.rateGate = rateGate;
        this.registry = registry;
        this.chatLogService = chatLogService;
        this.props = props;
        this.aiExecutor = aiExecutor;
    }

    public SseEmitter chat(Long userId, ChatReq req) {
        // 1) 参数校验 + 静默裁剪（40000 / 40400）
        List<Msg> messages = validator.validateAndTrim(req);
        // 2) 开关与 Key（50201）
        if (!props.isEnabled()) {
            throw new BusinessException(ResultCode.AI_UNAVAILABLE);
        }
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            log.warn("[ai] api-key 未配置，/ai/chat 直接 50201（05 §7）");
            throw new BusinessException(ResultCode.AI_UNAVAILABLE);
        }
        // 3) 闸门：3s 间隔 → 账号并发 → 全站并发
        RateGate.GateResult gate = rateGate.tryAcquire(userId);
        if (gate == RateGate.GateResult.USER_LIMIT) {
            throw new BusinessException(ResultCode.AI_USER_LIMIT);
        }
        if (gate == RateGate.GateResult.SITE_LIMIT) {
            throw new BusinessException(ResultCode.AI_SITE_LIMIT);
        }

        LocalDate today = QuotaService.today();
        boolean userQuota = false;
        boolean siteQuota = false;
        try {
            // 4) 预扣：账号额度 → 全站额度
            userQuota = quotaService.tryConsumeUser(userId, today, props.getQuota().getUserDaily());
            if (!userQuota) {
                throw new BusinessException(ResultCode.AI_USER_LIMIT);
            }
            siteQuota = quotaService.tryConsumeSite(today, props.getQuota().getSiteDaily());
            if (!siteQuota) {
                throw new BusinessException(ResultCode.AI_SITE_LIMIT);
            }

            // 5) 建流
            String messageId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
            AiStreamRegistry.StreamState state = registry.register(messageId, emitter);
            RunContext ctx = new RunContext(userId, req, messages, today, messageId, state, MDC.get("traceId"));
            registry.setTimeoutHandler(state, ignored -> onWatchdogTimeout(ctx));
            emitter.onCompletion(() -> {
                state.cancelled.set(true);
                if (ctx.status != 1) {
                    state.clientAborted.set(true);
                }
                registry.unregister(messageId);
            });
            emitter.onTimeout(() -> {
                state.cancelled.set(true);
                registry.cancel(state);
            });
            emitter.onError(error -> {
                state.clientAborted.set(true);
                state.cancelled.set(true);
                registry.cancel(state);
            });
            // 6) 提交线程池；拒绝(42902) 由外层统一回滚两项额度 + 释放全部 Semaphore
            try {
                aiExecutor.execute(() -> runStream(ctx));
            } catch (RejectedExecutionException ex) {
                log.warn("[ai] aiExecutor rejected, userId={}", userId);
                throw new BusinessException(ResultCode.AI_SITE_LIMIT);
            }
            return emitter;
        } catch (RuntimeException ex) {
            if (siteQuota) {
                quotaService.rollbackSite(today);
            }
            if (userQuota) {
                quotaService.rollbackUser(userId, today);
            }
            rateGate.release(userId);
            throw ex;
        }
    }

    /** worker 线程：只使用传入的 userId，禁止 StpUtil。 */
    private void runStream(RunContext ctx) {
        AiStreamRegistry.StreamState state = ctx.state;
        try {
            registry.sendMeta(state, props.getModel());
            List<UpstreamMsg> prompt = promptBuilder.build(ctx.questionId, ctx.messages);
            ctx.promptChars = prompt.stream()
                    .mapToInt(message -> message.content() == null ? 0 : message.content().length())
                    .sum();

            boolean sawDone = false;
            UpstreamSseParser.Usage usage = null;
            StringBuilder answer = new StringBuilder();
            try (InputStream upstream = llmStreamClient.open(prompt)) {
                registry.attachUpstream(state, upstream);
                BufferedReader reader = new BufferedReader(new InputStreamReader(upstream, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (state.cancelled.get()) {
                        break;
                    }
                    registry.touch(state);
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String payload = line.substring(5).trim();
                    if ("[DONE]".equals(payload)) {
                        sawDone = true;
                        break;
                    }
                    UpstreamSseParser.ParseResult parsed = parser.parse(payload);
                    // 讯飞每一行都带 code：HTTP 200 也可能是错误，必须先判 code
                    if (parsed.code() != 0) {
                        ctx.upstreamCode = parsed.code();
                        throw new UpstreamCodeException(parsed.code(), parsed.message());
                    }
                    if (parsed.securityHide()) {
                        // 审核片段：跳过该段，继续接收后续 delta（05 §9.3）
                        ctx.securityHit = true;
                        continue;
                    }
                    if (parsed.hasDelta()) {
                        registry.sendDelta(state, parsed.delta());
                        answer.append(parsed.delta());
                        ctx.completionChars = answer.length();
                        if (!state.firstTokenSent.get()) {
                            int firstTokenMs = (int) (System.currentTimeMillis() - ctx.startedAt);
                            ctx.firstTokenMs = firstTokenMs;
                            registry.markFirstToken(state, firstTokenMs);
                        }
                    }
                    if (parsed.hasUsage()) {
                        usage = parsed.usage();
                    }
                }
            } finally {
                registry.attachUpstream(state, null);
            }

            if (state.clientAborted.get()) {
                ctx.status = 3;
                return;
            }
            if (!sawDone) {
                throw new StreamBrokenException("upstream closed without [DONE]");
            }
            if (answer.length() == 0) {
                // 整条流没有有效内容（如全被审核屏蔽）：按首 token 前失败处理（05 §9.3）
                ctx.status = 2;
                ctx.errorCode = ResultCode.AI_UNAVAILABLE.getCode();
                registry.sendErrorQuietly(state, ctx.errorCode, ResultCode.AI_UNAVAILABLE.getMessage());
                return;
            }

            UpstreamSseParser.Usage finalUsage = usage != null
                    ? usage
                    : parser.estimateUsage(ctx.promptChars, answer.length());
            applyUsage(ctx, finalUsage);
            int usedAfter = quotaService.countUsed(ctx.userId, ctx.today);
            int limit = props.getQuota().getUserDaily();
            Map<String, Object> done = new LinkedHashMap<>();
            done.put("messageId", ctx.messageId);
            done.put("usage", usagePayload(finalUsage));
            done.put("quota", Map.of("used", usedAfter, "limit", limit));
            registry.sendDone(state, done);
            ctx.status = 1;
            consecutiveUpstreamFailures.set(0);
            if (limit > 0 && usedAfter >= Math.ceil(limit * QUOTA_WARN_RATIO)) {
                log.warn("[ai] user quota reached 80%: userId={} used={}/{}", ctx.userId, usedAfter, limit);
            }
        } catch (UpstreamCodeException ex) {
            int bizCode = upstreamCodeMapper.toBizCode(ex.code);
            ctx.status = 2;
            ctx.errorCode = bizCode;
            registry.sendErrorQuietly(state, bizCode, upstreamCodeMapper.userMessage(ex.code));
            logUpstreamCode(ctx, ex);
        } catch (LlmStreamClient.UpstreamTimeoutException ex) {
            ctx.status = 2;
            ctx.errorCode = ResultCode.AI_TIMEOUT.getCode();
            registry.sendErrorQuietly(state, ctx.errorCode, ResultCode.AI_TIMEOUT.getMessage());
            log.warn("[ai] upstream timeout before first token: userId={} cause={}", ctx.userId, ex.getMessage());
        } catch (LlmStreamClient.UpstreamHttpException ex) {
            int bizCode = upstreamCodeMapper.httpStatusToBizCode(ex.getStatus());
            ctx.status = 2;
            ctx.errorCode = bizCode;
            registry.sendErrorQuietly(state, bizCode, upstreamCodeMapper.httpStatusMessage(ex.getStatus()));
            log.warn("[ai] upstream http failed: userId={} status={}", ctx.userId, ex.getStatus());
        } catch (LlmStreamClient.UpstreamConnectException ex) {
            ctx.status = 2;
            ctx.errorCode = ResultCode.AI_UNAVAILABLE.getCode();
            registry.sendErrorQuietly(state, ctx.errorCode, ResultCode.AI_UNAVAILABLE.getMessage());
            log.warn("[ai] upstream connect failed: userId={} cause={}", ctx.userId, ex.getMessage());
        } catch (AiStreamRegistry.StreamCancelledException ex) {
            // 客户端断开：保留已收文本、计费，不算上游失败
            ctx.status = 3;
            state.clientAborted.set(true);
            log.debug("[ai] client aborted stream, messageId={}", ctx.messageId);
        } catch (StreamBrokenException ex) {
            ctx.status = 2;
            ctx.errorCode = ResultCode.AI_TIMEOUT.getCode();
            registry.sendErrorQuietly(state, ctx.errorCode, "回答中断，请重试");
            log.warn("[ai] upstream stream broken: userId={} cause={}", ctx.userId, ex.getMessage());
        } catch (IOException ex) {
            ctx.status = 2;
            ctx.errorCode = ResultCode.AI_TIMEOUT.getCode();
            registry.sendErrorQuietly(state, ctx.errorCode, "回答中断，请重试");
            log.warn("[ai] read upstream failed: userId={} cause={}", ctx.userId, ex.getMessage());
        } catch (RuntimeException ex) {
            ctx.status = 2;
            ctx.errorCode = ResultCode.AI_TIMEOUT.getCode();
            registry.sendErrorQuietly(state, ctx.errorCode, "回答中断，请重试");
            log.error("[ai] unexpected stream failure, userId={}", ctx.userId, ex);
        } finally {
            // 先收尾（写审计日志、释放信号量）再结束响应：客户端读到 EOF 时库里的 status 已落定
            finish(ctx);
            // 每一条流都必须显式 complete：否则客户端会一直等 EOF（done 只是事件，不会结束 HTTP 响应）
            registry.completeQuietly(state.emitter);
        }
    }

    /** watchdog 超时（05 §3.5）：发 50202 后收尾；与 worker 的 finish 用 finalized 保证只执行一次。 */
    private void onWatchdogTimeout(RunContext ctx) {
        if (ctx.state.finalized.get()) {
            return;
        }
        ctx.status = 2;
        ctx.errorCode = ResultCode.AI_TIMEOUT.getCode();
        registry.sendErrorQuietly(ctx.state, ctx.errorCode, ResultCode.AI_TIMEOUT.getMessage());
        registry.completeQuietly(ctx.state.emitter);
        log.warn("[ai] watchdog timeout: messageId={} firstTokenSent={}", ctx.messageId,
                ctx.state.firstTokenSent.get());
        finish(ctx);
    }

    /** 收尾：释放资源、按 05 §5.2 计费矩阵回滚/累计 token、写审计日志。只执行一次。 */
    private void finish(RunContext ctx) {
        if (!ctx.state.finalized.compareAndSet(false, true)) {
            return;
        }
        registry.unregister(ctx.messageId);
        if (ctx.released.compareAndSet(false, true)) {
            rateGate.release(ctx.userId);
        }

        boolean hasFirstToken = ctx.state.firstTokenSent.get();
        if (!hasFirstToken) {
            // 首 token 之前失败：账号/全站均不计，回滚
            quotaService.rollbackUser(ctx.userId, ctx.today);
            quotaService.rollbackSite(ctx.today);
        } else {
            if (ctx.totalTokens == 0) {
                // 已出字但未拿到 usage（断流/停止/失败）：按已收字符估算
                applyUsage(ctx, parser.estimateUsage(ctx.promptChars, ctx.completionChars));
            }
            quotaService.addSiteTokens(ctx.today, ctx.totalTokens);
        }
        if (ctx.status != 1 && ctx.status != 3) {
            recordUpstreamFailure();
        }

        long latency = System.currentTimeMillis() - ctx.startedAt;
        AiChatLog entry = new AiChatLog();
        entry.setUserId(ctx.userId);
        entry.setSessionId(ctx.sessionId);
        entry.setQuestionId(ctx.questionId);
        entry.setModel(props.getModel());
        entry.setPromptTokens(ctx.promptTokens);
        entry.setCompletionTokens(ctx.completionTokens);
        entry.setTotalTokens(ctx.totalTokens);
        entry.setEstimated(ctx.estimated);
        entry.setFirstTokenMs(ctx.firstTokenMs);
        entry.setLatencyMs((int) latency);
        entry.setStatus(ctx.status);
        entry.setErrorCode(ctx.errorCode);
        entry.setUpstreamCode(ctx.upstreamCode);
        chatLogService.log(entry);

        log.info("[ai] traceId={} userId={} sessionId={} questionId={} model={} promptTokens={} completionTokens={} "
                        + "totalTokens={} estimated={} firstTokenMs={} latencyMs={} status={} errorCode={} upstreamCode={} "
                        + "securityHit={}",
                ctx.traceId == null ? ctx.messageId : ctx.traceId, ctx.userId,
                ctx.sessionId == null ? "-" : ctx.sessionId, ctx.questionId, props.getModel(),
                ctx.promptTokens, ctx.completionTokens, ctx.totalTokens, ctx.estimated,
                ctx.firstTokenMs, latency, ctx.status, ctx.errorCode, ctx.upstreamCode, ctx.securityHit);
    }

    private void applyUsage(RunContext ctx, UpstreamSseParser.Usage usage) {
        ctx.promptTokens = usage.promptTokens();
        ctx.completionTokens = usage.completionTokens();
        ctx.totalTokens = usage.totalTokens();
        ctx.estimated = usage.estimated();
    }

    private Map<String, Object> usagePayload(UpstreamSseParser.Usage usage) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("promptTokens", usage.promptTokens());
        payload.put("completionTokens", usage.completionTokens());
        payload.put("totalTokens", usage.totalTokens());
        payload.put("estimated", usage.estimated());
        return payload;
    }

    private void logUpstreamCode(RunContext ctx, UpstreamCodeException ex) {
        log.warn("[ai] upstream code != 0: userId={} upstreamCode={} message={}", ctx.userId, ex.code, ex.getMessage());
        if (ex.code == 11200 || ex.code == 10014) {
            log.error("[ai] upstream authorization/content error, 检查 Key 与权限或输出审核：code={}", ex.code);
        }
        if (ex.code == 11201) {
            log.warn("[ai] 上游日流控超限（11201），建议临时关闭 ai.enabled，当日不要再打上游");
        }
    }

    private void recordUpstreamFailure() {
        int failures = consecutiveUpstreamFailures.incrementAndGet();
        if (failures >= UPSTREAM_FAILURE_ALERT_THRESHOLD) {
            log.error("[ai] 连续 {} 次上游失败，建议临时关闭 ai.enabled", failures);
        }
    }

    /** 一次流式请求的可变上下文（只在 worker / watchdog 收尾之间共享，finalized 保证只读一次）。 */
    private static final class RunContext {

        final Long userId;
        final ChatReq req;
        final List<Msg> messages;
        final LocalDate today;
        final String messageId;
        final AiStreamRegistry.StreamState state;
        final String traceId;
        final long startedAt = System.currentTimeMillis();
        final AtomicBoolean released = new AtomicBoolean(false);

        Long questionId;
        int status = 2;
        Integer errorCode;
        Integer upstreamCode;
        Integer firstTokenMs;
        volatile boolean securityHit;
        int promptChars;
        int completionChars;
        int promptTokens;
        int completionTokens;
        int totalTokens;
        int estimated;
        String sessionId;

        RunContext(Long userId, ChatReq req, List<Msg> messages, LocalDate today, String messageId,
                   AiStreamRegistry.StreamState state, String traceId) {
            this.userId = userId;
            this.req = req;
            this.messages = messages;
            this.today = today;
            this.messageId = messageId;
            this.state = state;
            this.traceId = traceId;
            this.questionId = req.getQuestionId();
            this.sessionId = req.getSessionId();
        }
    }

    private static class UpstreamCodeException extends RuntimeException {

        private final int code;

        UpstreamCodeException(int code, String message) {
            super(message);
            this.code = code;
        }
    }

    private static class StreamBrokenException extends RuntimeException {

        StreamBrokenException(String message) {
            super(message);
        }
    }
}
