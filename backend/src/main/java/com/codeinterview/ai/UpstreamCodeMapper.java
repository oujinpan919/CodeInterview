package com.codeinterview.ai;

import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * 讯飞上游 code → 本项目业务码（05 §9.1，唯一映射表）。
 * 禁止把上游原始 code 透给前端（用户看不懂，且泄漏上游信息）；原始 code 只进服务端日志。
 *
 * <p>一个 code 对应一个「业务码 + 用户文案」，集中在一张表里，避免 toBizCode / userMessage 两处 switch 漂移。
 */
@Component
public class UpstreamCodeMapper {

    private static final UpstreamError FALLBACK = new UpstreamError(50201, "AI 服务暂不可用");

    private static final Map<Integer, UpstreamError> ERRORS = Map.ofEntries(
            // 上一个问题未答完就发新请求
            Map.entry(10007, new UpstreamError(42901, "上一个问题还在回答中，请稍后再试")),
            // 输入内容审核不通过
            Map.entry(10013, new UpstreamError(40000, "内容不合规，请调整后重试")),
            // 输出内容审核不通过（流后）
            Map.entry(10014, new UpstreamError(50201, "回答被安全策略中断")),
            // 内容有违规倾向
            Map.entry(10019, new UpstreamError(40000, "内容不合规，请调整后重试")),
            // token 超上限（裁剪后仍超）
            Map.entry(10907, new UpstreamError(40000, "内容过长，请缩短后重试")),
            // 授权错误：appId 无权限 / 业务量超限
            Map.entry(11200, new UpstreamError(50201, "AI 服务暂不可用")),
            // 日流控超限
            Map.entry(11201, new UpstreamError(42902, "AI 服务今日繁忙，请明天再试")),
            // 秒级流控超限
            Map.entry(11202, new UpstreamError(42902, "AI 繁忙，请稍后重试")),
            // 并发流控超限
            Map.entry(11203, new UpstreamError(42902, "AI 繁忙，请稍后重试")));

    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    /** 上游 code（不是 HTTP 状态）→ 本项目业务码。 */
    public int toBizCode(int upstreamCode) {
        return ERRORS.getOrDefault(upstreamCode, FALLBACK).bizCode();
    }

    /** 上游 code → 用户文案（05 §9.1 / 04 B5.3）。 */
    public String userMessage(int upstreamCode) {
        return ERRORS.getOrDefault(upstreamCode, FALLBACK).message();
    }

    /** 授权类错误需要 ERROR 日志（检查 Key 与权限）。 */
    public boolean isAuthorizationError(int upstreamCode) {
        return upstreamCode == 11200;
    }

    /** 输出审核类错误（10014）需要 ERROR 日志 + 流后 error。 */
    public boolean isOutputModeration(int upstreamCode) {
        return upstreamCode == 10014;
    }

    /** 上游日流控（当日不要再打上游）。 */
    public boolean isDailyQuotaExceeded(int upstreamCode) {
        return upstreamCode == 11201;
    }

    /** 上游非 200 的 HTTP 状态 → 本项目 code（05 §9.2：429 按全站级 42902，其余 50201）。 */
    public int httpStatusToBizCode(int httpStatus) {
        return httpStatus == HTTP_TOO_MANY_REQUESTS ? 42902 : 50201;
    }

    public String httpStatusMessage(int httpStatus) {
        return httpStatus == HTTP_TOO_MANY_REQUESTS ? "AI 繁忙，请稍后重试" : "AI 服务暂不可用";
    }

    private record UpstreamError(int bizCode, String message) {
    }
}
