package com.codeinterview.chat.entity;

import lombok.Data;

/**
 * AI 调用审计日志（03 §3 第 11 张表 / migration-v5 §8）。
 * 只存用量与结果，不存会话正文（正文在 ai_message）。
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
    /** D6：关联会话；流前拒绝为 NULL。 */
    private Long conversationId;
    /** D7：USER_OWN / SITE_DEFAULT / GLOBAL_FALLBACK。 */
    private String resolvedFrom;
    private String providerType;
    /** D11：使用的 Prompt 模板。 */
    private Long templateId;
}
