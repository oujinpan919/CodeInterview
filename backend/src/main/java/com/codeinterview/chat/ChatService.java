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
    /** ai_chat_log.status（03 §3 第 11 张表 / 05 §11.1）。 */
    private static final int STATUS_OK = 1;
    private static final int STATUS_FAILED = 2;
    private static final int STATUS_CLIENT_ABORTED = 3;
    private static final String USER_MESSAGE_BROKEN = "回答中断，请重试";
    private static final String DATA_PREFIX = "data:";
    private static final String DONE_MARKER = "[DONE]";

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

    /** 入口：流前拒绝也必须有 INFO（05 §11.1「每次调用一条 INFO」，status=4 可选记录）。 */
    public SseEmitter chat(Long userId, ChatReq req) {
        try {
            return doChat(userId, req);
        } catch (BusinessException ex) {
            log.info("[ai] pre-stream reject userId={} questionId={} code={} message={}",
                    userId, req == null ? null : req.getQuestionId(),
                    ex.getResultCode().getCode(), ex.getMessage());
            throw ex;
        }
    }

    private SseEmitter doChat(Long userId, ChatReq req) {
        List<Msg> messages = precheck(req);
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
            userQuota = quotaService.tryConsumeUser(userId, today, props.getQuota().getUserDaily());
            if (!userQuota) {
                throw new BusinessException(ResultCode.AI_USER_LIMIT);
            }
            siteQuota = quotaService.tryConsumeSite(today, props.getQuota().getSiteDaily());
            if (!siteQuota) {
                throw new BusinessException(ResultCode.AI_SITE_LIMIT);
            }
            return startStream(userId, req, messages, today);
        } catch (RuntimeException ex) {
            rollbackQuota(userId, today, userQuota, siteQuota);
            rateGate.release(userId);
            throw ex;
        }
    }

    /** 1) 参数校验 + 静默裁剪；2) 开关与 Key（05 §3.2 step 1–2）。 */
    private List<Msg> precheck(ChatReq req) {
        List<Msg> messages = validator.validateAndTrim(req);
        if (!props.isEnabled()) {
            throw new BusinessException(ResultCode.AI_UNAVAILABLE);
        }
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            log.warn("[ai] api-key 未配置，/ai/chat 直接 50201（05 §7）");
            throw new BusinessException(ResultCode.AI_UNAVAILABLE);
        }
        return messages;
    }

    private void rollbackQuota(Long userId, LocalDate today, boolean userQuota, boolean siteQuota) {
        if (siteQuota) {
            quotaService.rollbackSite(today);
        }
        if (userQuota) {
            quotaService.rollbackUser(userId, today);
        }
    }

    /** 建流 + 提交线程池；线程池拒绝（42902）不建流，必须摘掉已登记的流并回滚（05 §3.2 step 6 / §5.3 第 8 步）。 */
    private SseEmitter startStream(Long userId, ChatReq req, List<Msg> messages, LocalDate today) {
        String messageId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        AiStreamRegistry.StreamState state = registry.register(messageId, emitter);
        RunContext ctx = new RunContext(userId, req, messages, today, messageId, state, MDC.get("traceId"));
        registry.setTimeoutHandler(state, ignored -> onWatchdogTimeout(ctx));
        registerEmitterCallbacks(emitter, state, ctx, messageId);
        try {
            aiExecutor.execute(() -> runStream(ctx));
        } catch (RejectedExecutionException ex) {
            // 这条 emitter 从未返回给 Spring：先摘流 + 标记已收尾，避免 180s 后 watchdog 重复回滚/写假审计
            state.cancelled.set(true);
            state.finalized.set(true);
            registry.unregister(messageId);
            log.warn("[ai] aiExecutor rejected, userId={}", userId);
            throw new BusinessException(ResultCode.AI_SITE_LIMIT);
        }
        return emitter;
    }

    private void registerEmitterCallbacks(SseEmitter emitter, AiStreamRegistry.StreamState state,
                                          RunContext ctx, String messageId) {
        emitter.onCompletion(() -> {
            state.cancelled.set(true);
            if (ctx.status != STATUS_OK) {
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
            completeStream(ctx, readUpstream(ctx, prompt));
        } catch (UpstreamCodeException ex) {
            failWithUpstreamCode(ctx, ex);
        } catch (LlmStreamClient.UpstreamTimeoutException ex) {
            fail(ctx, ResultCode.AI_TIMEOUT, ResultCode.AI_TIMEOUT.getMessage(), ex);
        } catch (LlmStreamClient.UpstreamHttpException ex) {
            failWithHttp(ctx, ex);
        } catch (LlmStreamClient.UpstreamConnectException ex) {
            fail(ctx, ResultCode.AI_UNAVAILABLE, ResultCode.AI_UNAVAILABLE.getMessage(), ex);
        } catch (AiStreamRegistry.StreamCancelledException ex) {
            // 客户端断开：保留已收文本、计费，不算上游失败
            ctx.status = STATUS_CLIENT_ABORTED;
            state.clientAborted.set(true);
            log.debug("[ai] client aborted stream, messageId={}", ctx.messageId);
        } catch (StreamBrokenException | IOException ex) {
            fail(ctx, ResultCode.AI_TIMEOUT, USER_MESSAGE_BROKEN, ex);
        } catch (RuntimeException ex) {
            log.error("[ai] unexpected stream failure, userId={}", ctx.userId, ex);
            fail(ctx, ResultCode.AI_TIMEOUT, USER_MESSAGE_BROKEN, ex);
        } finally {
            // 先收尾（写审计日志、释放信号量）再结束响应：客户端读到 EOF 时库里的 status 已落定
            finish(ctx);
            // 每一条流都必须显式 complete：否则客户端会一直等 EOF（done 只是事件，不会结束 HTTP 响应）
            registry.completeQuietly(state.emitter);
        }
    }

    /** 逐行读上游：每行先判 code；忽略 reasoning_content；HIDE_CONTINUE 跳过该段继续（05 §2.2 / §9.3）。 */
    private StreamRead readUpstream(RunContext ctx, List<UpstreamMsg> prompt) throws IOException {
        AiStreamRegistry.StreamState state = ctx.state;
        StringBuilder answer = new StringBuilder();
        UpstreamSseParser.Usage usage = null;
        boolean sawDone = false;
        try (InputStream upstream = llmStreamClient.open(prompt)) {
            registry.attachUpstream(state, upstream);
            BufferedReader reader = new BufferedReader(new InputStreamReader(upstream, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (state.cancelled.get()) {
                    break;
                }
                registry.touch(state);
                if (!line.startsWith(DATA_PREFIX)) {
                    continue;
                }
                String payload = line.substring(DATA_PREFIX.length()).trim();
                if (DONE_MARKER.equals(payload)) {
                    sawDone = true;
                    break;
                }
                UpstreamSseParser.ParseResult parsed = parser.parse(payload);
                if (parsed.sid() != null) {
                    ctx.sid = parsed.sid();
                }
                // 讯飞每一行都带 code：HTTP 200 也可能是错误，必须先判 code
                if (parsed.code() != 0) {
                    ctx.upstreamCode = parsed.code();
                    throw new UpstreamCodeException(parsed.code(), parsed.message());
                }
                if (parsed.securityHide()) {
                    ctx.securityHit = true;
                    continue;
                }
                appendDelta(ctx, answer, parsed);
                if (parsed.hasUsage()) {
                    usage = parsed.usage();
                }
            }
        } finally {
            registry.attachUpstream(state, null);
        }
        return new StreamRead(sawDone, usage, answer.toString());
    }

    private void appendDelta(RunContext ctx, StringBuilder answer, UpstreamSseParser.ParseResult parsed) {
        if (!parsed.hasDelta()) {
            return;
        }
        registry.sendDelta(ctx.state, parsed.delta());
        answer.append(parsed.delta());
        ctx.completionChars = answer.length();
        if (!ctx.state.firstTokenSent.get()) {
            int firstTokenMs = (int) (System.currentTimeMillis() - ctx.startedAt);
            ctx.firstTokenMs = firstTokenMs;
            registry.markFirstToken(ctx.state, firstTokenMs);
        }
    }

    /** 正常结束：客户端已断开→status=3；上游无 [DONE]→中断；空回答按首 token 前失败；否则发 done。 */
    private void completeStream(RunContext ctx, StreamRead read) {
        if (ctx.state.clientAborted.get()) {
            ctx.status = STATUS_CLIENT_ABORTED;
            return;
        }
        if (!read.sawDone()) {
            throw new StreamBrokenException("upstream closed without " + DONE_MARKER);
        }
        if (read.answer().isEmpty()) {
            // 整条流没有有效内容（如全被审核屏蔽）：按首 token 前失败处理（05 §9.3）
            fail(ctx, ResultCode.AI_UNAVAILABLE, ResultCode.AI_UNAVAILABLE.getMessage(), null);
            return;
        }
        UpstreamSseParser.Usage finalUsage = read.usage() != null
                ? read.usage()
                : parser.estimateUsage(ctx.promptChars, read.answer().length());
        applyUsage(ctx, finalUsage);
        int usedAfter = quotaService.countUsed(ctx.userId, ctx.today);
        int limit = props.getQuota().getUserDaily();
        registry.sendDone(ctx.state, donePayload(ctx.messageId, finalUsage, usedAfter, limit));
        ctx.status = STATUS_OK;
        consecutiveUpstreamFailures.set(0);
        if (limit > 0 && usedAfter >= Math.ceil(limit * QUOTA_WARN_RATIO)) {
            log.warn("[ai] user quota reached 80%: userId={} used={}/{}", ctx.userId, usedAfter, limit);
        }
    }

    private Map<String, Object> donePayload(String messageId, UpstreamSseParser.Usage usage, int used, int limit) {
        Map<String, Object> done = new LinkedHashMap<>();
        done.put("messageId", messageId);
        done.put("usage", usagePayload(usage));
        done.put("quota", Map.of("used", used, "limit", limit));
        return done;
    }

    // ---- 失败收尾 ----

    private void fail(RunContext ctx, ResultCode resultCode, String message, Throwable cause) {
        failWithCode(ctx, resultCode.getCode(), message, cause);
    }

    private void failWithCode(RunContext ctx, int bizCode, String message, Throwable cause) {
        ctx.status = STATUS_FAILED;
        ctx.errorCode = bizCode;
        registry.sendErrorQuietly(ctx.state, bizCode, message);
        log.warn("[ai] stream failed: userId={} code={} cause={}", ctx.userId, bizCode,
                cause == null ? message : cause.getMessage());
    }

    private void failWithUpstreamCode(RunContext ctx, UpstreamCodeException ex) {
        failWithCode(ctx, upstreamCodeMapper.toBizCode(ex.code),
                upstreamCodeMapper.userMessage(ex.code), ex);
        logUpstreamCode(ctx, ex);
    }

    private void failWithHttp(RunContext ctx, LlmStreamClient.UpstreamHttpException ex) {
        failWithCode(ctx, upstreamCodeMapper.httpStatusToBizCode(ex.getStatus()),
                upstreamCodeMapper.httpStatusMessage(ex.getStatus()), ex);
        log.warn("[ai] upstream http failed: userId={} status={}", ctx.userId, ex.getStatus());
    }

    /** watchdog 超时（05 §3.5）：发 50202 后收尾；与 worker 的 finish 用 finalized 保证只执行一次。 */
    private void onWatchdogTimeout(RunContext ctx) {
        if (ctx.state.finalized.get()) {
            return;
        }
        fail(ctx, ResultCode.AI_TIMEOUT, ResultCode.AI_TIMEOUT.getMessage(), null);
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
        if (ctx.status != STATUS_OK && ctx.status != STATUS_CLIENT_ABORTED) {
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
                        + "sid={} securityHit={}",
                ctx.traceId == null ? ctx.messageId : ctx.traceId, ctx.userId,
                ctx.sessionId == null ? "-" : ctx.sessionId, ctx.questionId, props.getModel(),
                ctx.promptTokens, ctx.completionTokens, ctx.totalTokens, ctx.estimated,
                ctx.firstTokenMs, latency, ctx.status, ctx.errorCode, ctx.upstreamCode,
                ctx.sid == null ? "-" : ctx.sid, ctx.securityHit);
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
        if (upstreamCodeMapper.isAuthorizationError(ex.code) || upstreamCodeMapper.isOutputModeration(ex.code)) {
            log.error("[ai] upstream authorization/content error, 检查 Key 与权限或输出审核：code={}", ex.code);
        }
        if (upstreamCodeMapper.isDailyQuotaExceeded(ex.code)) {
            log.warn("[ai] 上游日流控超限，建议临时关闭 ai.enabled，当日不要再打上游");
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
        final List<Msg> messages;
        final LocalDate today;
        final String messageId;
        final AiStreamRegistry.StreamState state;
        final String traceId;
        final long startedAt = System.currentTimeMillis();
        final AtomicBoolean released = new AtomicBoolean(false);

        Long questionId;
        String sessionId;
        String sid;
        int status = STATUS_FAILED;
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

        RunContext(Long userId, ChatReq req, List<Msg> messages, LocalDate today, String messageId,
                   AiStreamRegistry.StreamState state, String traceId) {
            this.userId = userId;
            this.messages = messages;
            this.today = today;
            this.messageId = messageId;
            this.state = state;
            this.traceId = traceId;
            this.questionId = req.getQuestionId();
            this.sessionId = req.getSessionId();
        }
    }

    /** 读流结果：正常结束标记 + 上游 usage（可能缺失）+ 已收正文。 */
    private record StreamRead(boolean sawDone, UpstreamSseParser.Usage usage, String answer) {
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
