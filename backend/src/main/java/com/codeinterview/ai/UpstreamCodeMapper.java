package com.codeinterview.ai;

import org.springframework.stereotype.Component;

/**
 * 讯飞上游 code → 本项目业务码（05 §9.1，唯一映射表）。
 * 禁止把上游原始 code 透给前端（用户看不懂，且泄漏上游信息）；原始 code 只进服务端日志。
 */
@Component
public class UpstreamCodeMapper {

    public static final int UNKNOWN_UPSTREAM_CODE = -1;

    /** 上游 code（不是 HTTP 状态）→ 本项目 code。 */
    public int toBizCode(int upstreamCode) {
        return switch (upstreamCode) {
            case 10007 -> 42901;
            case 10013, 10019, 10907 -> 40000;
            case 10014 -> 50201;
            case 11200 -> 50201;
            case 11201, 11202, 11203 -> 42902;
            default -> 50201;
        };
    }

    /** 上游 code → 用户文案（05 §9.1 / 04 B5.3）。 */
    public String userMessage(int upstreamCode) {
        return switch (upstreamCode) {
            case 10007 -> "上一个问题还在回答中，请稍后再试";
            case 10013, 10019 -> "内容不合规，请调整后重试";
            case 10907 -> "内容过长，请缩短后重试";
            case 10014 -> "回答被安全策略中断";
            case 11200 -> "AI 服务暂不可用";
            case 11201 -> "AI 服务今日繁忙，请明天再试";
            case 11202, 11203 -> "AI 繁忙，请稍后重试";
            default -> "AI 服务暂不可用";
        };
    }

    /** 上游非 200 的 HTTP 状态 → 本项目 code（05 §9.2）。 */
    public int httpStatusToBizCode(int httpStatus) {
        if (httpStatus == 429) {
            return 42902;
        }
        return 50201;
    }

    public String httpStatusMessage(int httpStatus) {
        return httpStatus == 429 ? "AI 繁忙，请稍后重试" : "AI 服务暂不可用";
    }
}
