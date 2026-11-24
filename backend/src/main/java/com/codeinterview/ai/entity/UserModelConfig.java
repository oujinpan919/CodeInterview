package com.codeinterview.ai.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 用户级模型配置（08 §6 / db migration-v5 §2）。
 * 软删：{@code deleted=1}；D14 禁止删除被会话引用的配置。
 */
@Data
@TableName("user_model_config")
public class UserModelConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
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
    private Integer isDefault;
    private Integer isEnabled;
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
