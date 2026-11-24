package com.codeinterview.ai.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 站点级模型配置（D2 / migration-v5 §1）。管理员维护，全站至多 1 条 is_site_default。
 */
@Data
@TableName("site_model_config")
public class SiteModelConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String providerType;
    private String displayName;
    private String baseUrl;
    private String accessMode;
    private String modelId;
    private String modelDisplay;
    private Long credentialRef;
    private BigDecimal temperature;
    private Integer maxTokens;
    private Integer thinkingEnabled;
    private Integer timeoutMs;
    private String extraBody;
    private String testStatus;
    private LocalDateTime lastTestedAt;
    private Integer lastTestLatencyMs;
    private String lastError;
    private Integer isSiteDefault;
    private Integer isEnabled;
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
