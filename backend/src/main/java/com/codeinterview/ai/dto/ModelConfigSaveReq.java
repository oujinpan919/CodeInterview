package com.codeinterview.ai.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;

/**
 * 模型配置保存请求（08 §6 / B2 单表单）。
 * {@code apiKey} 为空表示「不修改凭证」，非空则覆盖。
 *
 * <p>v3.24：补齐 Bean Validation。此前该 DTO 无任何约束注解，而 Controller 上已经写了
 * {@code @Valid} —— 等于注解空转，所有校验只能靠 {@code UserModelConfigService.validate()}
 * 手工兜底，且超长字段会一路写到 VARCHAR 截断为止。现在入口统一拦截，
 * 由 {@code GlobalExceptionHandler#handleMethodArgumentNotValid} 转成 40000 + 字段级文案。
 *
 * <p>长度上限与建表定义对齐：display_name(32) / base_url(512) / model_id(128) /
 * model_display(64) / provider_type(32) / access_mode(16)。
 */
@Data
public class ModelConfigSaveReq {

    @Size(max = 32, message = "providerType 不能超过 32 字符")
    private String providerType;

    @NotBlank(message = "请填写展示名")
    @Size(max = 32, message = "展示名不能超过 32 字")
    private String displayName;

    @NotBlank(message = "请填写接口地址（Base URL）")
    @Size(max = 512, message = "接口地址过长（最多 512 字符）")
    @Pattern(regexp = "^https?://\\S+$", message = "接口地址需以 http:// 或 https:// 开头")
    private String baseUrl;

    @Size(max = 16, message = "accessMode 不能超过 16 字符")
    private String accessMode;

    @NotBlank(message = "请填写模型 ID")
    @Size(max = 128, message = "模型 ID 不能超过 128 字符")
    private String modelId;

    @Size(max = 64, message = "模型展示名不能超过 64 字")
    private String modelDisplay;

    @Size(max = 512, message = "API Key 过长（最多 512 字符）")
    private String apiKey;

    @DecimalMin(value = "0.0", message = "temperature 需在 0–2 之间")
    @DecimalMax(value = "2.0", message = "temperature 需在 0–2 之间")
    private Double temperature;

    @Min(value = 1, message = "max_tokens 需在 1–8000 之间")
    @Max(value = 8000, message = "max_tokens 需在 1–8000 之间")
    private Integer maxTokens;

    private Boolean thinkingEnabled;

    @Min(value = 1000, message = "超时需在 1000–120000 毫秒之间")
    @Max(value = 120000, message = "超时需在 1000–120000 毫秒之间")
    private Integer timeoutMs;

    @Size(max = 2000, message = "extraBody 过长（最多 2000 字符）")
    private String extraBody;

    private Boolean isEnabled;

    private Boolean makeDefault;
}
