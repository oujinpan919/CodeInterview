package com.codeinterview.ai.config;

import org.springframework.stereotype.Component;

import com.codeinterview.ai.AiProperties;
import com.codeinterview.ai.entity.SiteModelConfig;
import com.codeinterview.ai.entity.UserModelConfig;
import com.codeinterview.ai.mapper.SiteModelConfigMapper;
import com.codeinterview.ai.mapper.SiteSettingMapper;
import com.codeinterview.ai.mapper.UserModelConfigMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 模型配置解析链（08 §5「解析优先级」/ D2 / D7）。
 *
 * <pre>
 * 用户 is_default ∧ is_enabled
 *   → 用户任一 is_enabled
 *   → 站点 is_site_default
 *   → 站点任一 is_enabled
 *   → 全局 ai.*（GLOBAL_FALLBACK）
 *   → 50201
 * </pre>
 *
 * <p>{@code resolvedFrom} 是 D7 计费分叉的唯一依据：
 * 只有 {@code SITE_DEFAULT} 与 {@code GLOBAL_FALLBACK} 才消耗站点预算，
 * {@code USER_OWN} 不占 {@code site_ai_usage.used_count}。
 *
 * <p>{@code allow_user_own_key=false} 时跳过用户级两步，但<b>不删除</b>已存配置
 * （A7：开关重新打开后自动恢复）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelConfigResolver {

    public static final String KEY_ALLOW_USER_OWN_KEY = "allow_user_own_key";

    public enum ResolvedFrom {
        USER_OWN,
        SITE_DEFAULT,
        GLOBAL_FALLBACK
    }

    /** 解析结果：统一成一份可下发上游的配置。 */
    public record Resolved(long configId,
                           ResolvedFrom from,
                           String providerType,
                           String displayName,
                           String baseUrl,
                           String modelId,
                           Long credentialRef,
                           Double temperature,
                           Integer maxTokens,
                           Boolean thinkingEnabled,
                           Integer timeoutMs,
                           String extraBody) {

        /** 是否消耗全站预算（D7）。 */
        public boolean consumesSiteQuota() {
            return from != ResolvedFrom.USER_OWN;
        }

        public boolean isUserOwn() {
            return from == ResolvedFrom.USER_OWN;
        }
    }

    private final UserModelConfigMapper userConfigMapper;
    private final SiteModelConfigMapper siteConfigMapper;
    private final SiteSettingMapper siteSettingMapper;
    private final AiProperties props;

    /** 是否允许用户自带 Key（缺省允许，与 seed 一致）。 */
    public boolean allowUserOwnKey() {
        String value = siteSettingMapper.selectValue(KEY_ALLOW_USER_OWN_KEY);
        return value == null || !"0".equals(value.trim());
    }

    public void setAllowUserOwnKey(boolean allowed) {
        siteSettingMapper.upsert(KEY_ALLOW_USER_OWN_KEY, allowed ? "1" : "0",
                "是否允许用户配置并使用自带的模型凭证（D2 开关）");
    }

    /**
     * 解析当前生效配置；返回 {@code null} 表示连全局兜底都没有（调用方转 50201）。
     *
     * @param configId 可选：用户在下拉里显式选择的配置 id；为空则按优先级链自动选
     */
    public Resolved resolve(Long userId, Long configId) {
        if (allowUserOwnKey()) {
            if (configId != null) {
                UserModelConfig picked = userConfigMapper.selectOwned(configId, userId);
                if (picked != null && isEnabled(picked.getIsEnabled())) {
                    return fromUser(picked);
                }
            }
            UserModelConfig active = userConfigMapper.selectActive(userId);
            if (active != null) {
                return fromUser(active);
            }
        }

        SiteModelConfig site = siteConfigMapper.selectActive();
        if (site != null) {
            return fromSite(site);
        }

        return fromGlobal();
    }

    /** 未配置任何模型时，前端据此展示「还没有可用的模型，去配置」。 */
    public boolean hasAnyUsableConfig(Long userId) {
        if (allowUserOwnKey() && userConfigMapper.selectActive(userId) != null) {
            return true;
        }
        if (siteConfigMapper.selectActive() != null) {
            return true;
        }
        return props.getApiKey() != null && !props.getApiKey().isBlank();
    }

    private Resolved fromUser(UserModelConfig config) {
        return new Resolved(
                config.getId(),
                ResolvedFrom.USER_OWN,
                config.getProviderType(),
                config.getDisplayName(),
                config.getBaseUrl(),
                config.getModelId(),
                config.getCredentialRef(),
                config.getTemperature() == null ? null : config.getTemperature().doubleValue(),
                config.getMaxTokens(),
                isEnabled(config.getThinkingEnabled()),
                config.getTimeoutMs(),
                config.getExtraBody());
    }

    private Resolved fromSite(SiteModelConfig config) {
        return new Resolved(
                config.getId(),
                ResolvedFrom.SITE_DEFAULT,
                config.getProviderType(),
                config.getDisplayName(),
                config.getBaseUrl(),
                config.getModelId(),
                config.getCredentialRef(),
                config.getTemperature() == null ? null : config.getTemperature().doubleValue(),
                config.getMaxTokens(),
                isEnabled(config.getThinkingEnabled()),
                config.getTimeoutMs(),
                config.getExtraBody());
    }

    /** 全局 ai.*：id 用 0 表示「非表记录」，凭据为空表示用系统 Key。 */
    private Resolved fromGlobal() {
        String apiKey = props.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("[ai-config] no usable model config and global api-key missing");
            return null;
        }
        return new Resolved(
                0L,
                ResolvedFrom.GLOBAL_FALLBACK,
                props.getProvider(),
                "站点默认（全局配置）",
                props.getBaseUrl(),
                props.getModel(),
                null,
                props.getTemperature(),
                props.getMaxTokens(),
                props.isThinkingEnabled(),
                null,
                null);
    }

    private boolean isEnabled(Integer flag) {
        return flag != null && flag == 1;
    }
}
