package com.codeinterview.chat.entity;

import lombok.Data;

/**
 * AI 调用审计日志（03 §3 第 11 张表）。只存用量与结果，不存会话正文。
 */
@Data
public class AiChatLog {

    private Long userId;
    private String sessionId;
    private Long questionId;
    private String model;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Integer estimated;
    private Integer firstTokenMs;
    private Integer latencyMs;
    private Integer status;
    private Integer errorCode;
    private Integer upstreamCode;
}
