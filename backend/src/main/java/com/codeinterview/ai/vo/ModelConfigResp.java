package com.codeinterview.ai.vo;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 模型配置出参（08 §6）。凭证只回掩码，永不回显明文。
 */
@Data
public class ModelConfigResp {

    private Long id;
    private String providerType;
    private String displayName;
    private String baseUrl;
    private String accessMode;
    private String modelId;
    private String modelDisplay;
    private Double temperature;
    private Integer maxTokens;
    private Boolean thinkingEnabled;
    private Integer timeoutMs;
    private String extraBody;
    private String testStatus;
    private LocalDateTime lastTestedAt;
    private Integer lastTestLatencyMs;
    private String lastError;
    private Boolean isDefault;
    private Boolean isEnabled;
    /** 掩码提示，如 ak-****1b2；无凭证为空串。 */
    private String apiKeyHint;
    private Boolean hasCredential;
}
