package com.codeinterview.ai.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codeinterview.ai.config.ConnectionTester;
import com.codeinterview.ai.config.ModelConfigResolver;
import com.codeinterview.ai.config.OutboundUrlGuard;
import com.codeinterview.ai.dto.ConnectTestReq;
import com.codeinterview.ai.dto.ModelConfigSaveReq;
import com.codeinterview.ai.entity.AiConfigAudit;
import com.codeinterview.ai.entity.AiCredential;
import com.codeinterview.ai.entity.UserModelConfig;
import com.codeinterview.ai.mapper.AiConfigAuditMapper;
import com.codeinterview.ai.mapper.UserModelConfigMapper;
import com.codeinterview.ai.vo.ConnectTestResp;
import com.codeinterview.ai.vo.ModelConfigResp;
import com.codeinterview.common.exception.BusinessException;
import com.codeinterview.common.result.ResultCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户级模型配置（HC-3/4、D2、D13、D14）。
 *
 * <p>关键约束：
 * <ul>
 *   <li>所有读写都按登录态 {@code userId} 过滤（08 §7 S3 越权）；</li>
 *   <li>启用数上限 5（D13）；</li>
 *   <li>被 {@code ai_conversation} 引用时只允许停用不允许删除（D14）；</li>
 *   <li>{@code allow_user_own_key=false} 时拒绝新增/修改（A7：不删除已有数据）。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserModelConfigService {

    public static final int MAX_ENABLED = 5;

    private final UserModelConfigMapper configMapper;
    private final AiConfigAuditMapper auditMapper;
    private final CredentialService credentialService;
    private final OutboundUrlGuard urlGuard;
    private final ConnectionTester connectionTester;
    private final ModelConfigResolver resolver;

    public List<ModelConfigResp> list(Long userId) {
        List<UserModelConfig> configs = configMapper.selectAll(userId);
        List<ModelConfigResp> result = new ArrayList<>(configs.size());
        for (UserModelConfig config : configs) {
            result.add(toResp(config));
        }
        return result;
    }

    public ModelConfigResp detail(Long userId, Long id) {
        return toResp(requireOwned(userId, id));
    }

    @Transactional
    public ModelConfigResp create(Long userId, ModelConfigSaveReq req) {
        requireUserOwnKeyAllowed();
        String normalizedUrl = validate(req);
        if (req.getApiKey() == null || req.getApiKey().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "用户自带模型必须填写 API Key；如需使用站点模型，请保持站点默认配置");
        }

        long enabledCount = configMapper.countEnabled(userId);
        boolean wantEnabled = req.getIsEnabled() == null || req.getIsEnabled();
        if (wantEnabled && enabledCount >= MAX_ENABLED) {
            throw new BusinessException(ResultCode.AI_CONFIG_LIMIT,
                    "最多启用 " + MAX_ENABLED + " 个模型，请先停用其他配置");
        }

        ensureDisplayNameFree(userId, req.getDisplayName(), null);

        UserModelConfig config = new UserModelConfig();
        config.setUserId(userId);
        applyFields(config, req, normalizedUrl);
        config.setIsEnabled(wantEnabled ? 1 : 0);
        config.setIsDefault(0);
        config.setDeleted(0);
        config.setTestStatus("untested");

        if (req.getApiKey() != null && !req.getApiKey().isBlank()) {
            config.setCredentialRef(credentialService.save(null, AiCredential.OWNER_USER, userId, req.getApiKey().trim()));
        }
        configMapper.insert(config);

        if (Boolean.TRUE.equals(req.getMakeDefault())) {
            applyDefault(userId, config.getId());
        }
        audit(userId, config.getId(), AiConfigAudit.ACTION_CREATE, req.getDisplayName());
        return toResp(configMapper.selectById(config.getId()));
    }

    @Transactional
    public ModelConfigResp update(Long userId, Long id, ModelConfigSaveReq req) {
        requireUserOwnKeyAllowed();
        UserModelConfig existing = requireOwned(userId, id);
        String normalizedUrl = validate(req);
        ensureDisplayNameFree(userId, req.getDisplayName(), id);
        if ((req.getApiKey() == null || req.getApiKey().isBlank())
                && existing.getCredentialRef() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "该模型尚未配置 API Key，请先填写；站点 Key 不能用于用户自填接口地址");
        }

        boolean wantEnabled = req.getIsEnabled() == null
                ? existing.getIsEnabled() == 1
                : req.getIsEnabled();
        if (wantEnabled && existing.getIsEnabled() == 0) {
            if (configMapper.countEnabled(userId) >= MAX_ENABLED) {
                throw new BusinessException(ResultCode.AI_CONFIG_LIMIT,
                        "最多启用 " + MAX_ENABLED + " 个模型，请先停用其他配置");
            }
        }

        applyFields(existing, req, normalizedUrl);
        existing.setIsEnabled(wantEnabled ? 1 : 0);
        if (existing.getIsEnabled() == 0) {
            // 停用后不能继续当默认
            existing.setIsDefault(0);
        }

        if (req.getApiKey() != null && !req.getApiKey().isBlank()) {
            existing.setCredentialRef(credentialService.save(existing.getCredentialRef(),
                    AiCredential.OWNER_USER, userId, req.getApiKey().trim()));
            // 凭证变了，之前的测试结论失效
            existing.setTestStatus("untested");
        }
        configMapper.updateById(existing);

        if (Boolean.TRUE.equals(req.getMakeDefault()) && existing.getIsEnabled() == 1) {
            applyDefault(userId, id);
        }
        audit(userId, id, AiConfigAudit.ACTION_UPDATE, req.getDisplayName());
        return toResp(configMapper.selectById(id));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        UserModelConfig existing = requireOwned(userId, id);
        if (configMapper.countReferenced(id) > 0) {
            // D14：被会话引用 → 拒绝删除，提示改为停用
            throw new BusinessException(ResultCode.REFERENCE_CONFLICT,
                    "该配置已被历史会话引用，无法删除；可改为停用");
        }
        configMapper.deleteById(id);
        credentialService.delete(existing.getCredentialRef());
        audit(userId, id, AiConfigAudit.ACTION_DELETE, existing.getDisplayName());
    }

    @Transactional
    public void setDefault(Long userId, Long id) {
        UserModelConfig existing = requireOwned(userId, id);
        if (existing.getIsEnabled() != 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请先启用该配置再设为默认");
        }
        applyDefault(userId, id);
        audit(userId, id, AiConfigAudit.ACTION_SET_DEFAULT, existing.getDisplayName());
    }

    @Transactional
    public void toggle(Long userId, Long id, boolean enabled) {
        requireUserOwnKeyAllowed();
        UserModelConfig existing = requireOwned(userId, id);
        if (enabled && existing.getIsEnabled() == 0
                && configMapper.countEnabled(userId) >= MAX_ENABLED) {
            throw new BusinessException(ResultCode.AI_CONFIG_LIMIT,
                    "最多启用 " + MAX_ENABLED + " 个模型，请先停用其他配置");
        }
        existing.setIsEnabled(enabled ? 1 : 0);
        if (!enabled) {
            existing.setIsDefault(0);
        }
        configMapper.updateById(existing);
        audit(userId, id, AiConfigAudit.ACTION_UPDATE, enabled ? "启用" : "停用");
    }

    /** 连接测试（D4）。未保存的草稿也可测；已存配置可复用已保存凭证。 */
    public ConnectTestResp test(Long userId, ConnectTestReq req) {
        String resolvedKey = req.getApiKey() == null || req.getApiKey().isBlank()
                ? lookupKey(userId, req.getConfigId())
                : req.getApiKey();
        ConnectionTester.Result result = connectionTester.test(req, resolvedKey);

        // 已保存的配置顺便落测试状态
        if (req.getConfigId() != null) {
            UserModelConfig existing = configMapper.selectOwned(req.getConfigId(), userId);
            if (existing != null) {
                existing.setTestStatus(result.statusId());
                existing.setLastTestedAt(java.time.LocalDateTime.now());
                existing.setLastTestLatencyMs((int) result.latencyMs());
                existing.setLastError(result.ok() ? null : truncate(result.error()));
                configMapper.updateById(existing);
                audit(userId, existing.getId(), AiConfigAudit.ACTION_TEST, result.statusId());
            }
        }

        ConnectTestResp resp = new ConnectTestResp();
        resp.setStatus(result.statusId());
        resp.setOk(result.ok());
        resp.setLatencyMs(result.latencyMs());
        resp.setModels(result.models());
        resp.setSuggestV1(result.suggestV1());
        resp.setError(result.error());
        return resp;
    }

    /** 从已存配置取凭证明文（测试续用）。 */
    private String lookupKey(Long userId, Long configId) {
        if (configId == null) {
            return null;
        }
        UserModelConfig existing = configMapper.selectOwned(configId, userId);
        return existing == null ? null : credentialService.plaintext(existing.getCredentialRef());
    }

    private void applyDefault(Long userId, Long id) {
        configMapper.clearDefault(userId);
        configMapper.setDefault(id, userId);
    }

    private void applyFields(UserModelConfig config, ModelConfigSaveReq req, String normalizedUrl) {
        config.setProviderType(defaultIfBlank(req.getProviderType(), "custom"));
        config.setDisplayName(req.getDisplayName().trim());
        config.setBaseUrl(normalizedUrl);
        config.setAccessMode(defaultIfBlank(req.getAccessMode(), "direct"));
        config.setModelId(req.getModelId().trim());
        config.setModelDisplay(req.getModelDisplay() == null || req.getModelDisplay().isBlank()
                ? req.getModelId().trim()
                : req.getModelDisplay().trim());
        config.setTemperature(req.getTemperature() == null
                ? new java.math.BigDecimal("0.30")
                : java.math.BigDecimal.valueOf(req.getTemperature()));
        config.setMaxTokens(req.getMaxTokens() == null ? 1200 : req.getMaxTokens());
        config.setThinkingEnabled(Boolean.TRUE.equals(req.getThinkingEnabled()) ? 1 : 0);
        config.setTimeoutMs(req.getTimeoutMs());
        config.setExtraBody(normalizeJson(req.getExtraBody()));
    }

    /**
     * 校验并返回规范化后的接口地址。
     *
     * <p>v3.24：除原有必填/范围校验外，补齐了与建表定义对齐的长度上限
     * （model_id 128 / model_display 64 / api_key 512 / timeout_ms 范围）——
     * 此前这些超长值会一路写到 VARCHAR，被 MySQL 静默截断或直接报 50000。
     * 格式类约束（协议头、长度）已由 {@link ModelConfigSaveReq} 的 Bean Validation 拦截，
     * 这里是服务层兜底，非 Controller 入口（如内部调用）同样生效。
     */
    private String validate(ModelConfigSaveReq req) {
        if (req.getDisplayName() == null || req.getDisplayName().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请填写展示名");
        }
        if (req.getDisplayName().trim().length() > 32) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "展示名不能超过 32 字");
        }
        if (req.getModelId() == null || req.getModelId().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请填写模型 ID");
        }
        if (req.getModelId().trim().length() > 128) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "模型 ID 不能超过 128 字符");
        }
        if (req.getModelDisplay() != null && req.getModelDisplay().trim().length() > 64) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "模型展示名不能超过 64 字");
        }
        if (req.getApiKey() != null && req.getApiKey().length() > 512) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "API Key 过长（最多 512 字符）");
        }
        if (req.getMaxTokens() != null && (req.getMaxTokens() <= 0 || req.getMaxTokens() > 8000)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "max_tokens 需在 1–8000 之间");
        }
        if (req.getTemperature() != null && (req.getTemperature() < 0 || req.getTemperature() > 2)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "temperature 需在 0–2 之间");
        }
        if (req.getTimeoutMs() != null && (req.getTimeoutMs() < 1000 || req.getTimeoutMs() > 120000)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "超时需在 1000–120000 毫秒之间");
        }
        // base_url 的 SSRF 校验
        return urlGuard.validate(req.getBaseUrl()).normalizedUrl();
    }

    private void ensureDisplayNameFree(Long userId, String displayName, Long excludeId) {
        List<UserModelConfig> configs = configMapper.selectAll(userId);
        String target = displayName.trim();
        for (UserModelConfig config : configs) {
            if (excludeId != null && excludeId.equals(config.getId())) {
                continue;
            }
            if (target.equals(config.getDisplayName())) {
                throw new BusinessException(ResultCode.CONFLICT, "该展示名已存在，请换一个");
            }
        }
    }

    private void requireUserOwnKeyAllowed() {
        if (!resolver.allowUserOwnKey()) {
            throw new BusinessException(ResultCode.AI_CONFIG_DISABLED);
        }
    }

    private UserModelConfig requireOwned(Long userId, Long id) {
        UserModelConfig config = configMapper.selectOwned(id, userId);
        if (config == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return config;
    }

    private void audit(Long userId, Long targetId, String action, String detail) {
        AiConfigAudit entry = new AiConfigAudit();
        entry.setUserId(userId);
        entry.setTargetType(AiConfigAudit.TARGET_USER);
        entry.setTargetId(targetId);
        entry.setAction(action);
        entry.setDetail(detail == null ? "" : truncate(detail));
        try {
            auditMapper.insert(entry);
        } catch (Exception ex) {
            log.warn("[ai-config] write audit failed: {}", ex.getMessage());
        }
    }

    private ModelConfigResp toResp(UserModelConfig config) {
        ModelConfigResp resp = new ModelConfigResp();
        resp.setId(config.getId());
        resp.setProviderType(config.getProviderType());
        resp.setDisplayName(config.getDisplayName());
        resp.setBaseUrl(config.getBaseUrl());
        resp.setAccessMode(config.getAccessMode());
        resp.setModelId(config.getModelId());
        resp.setModelDisplay(config.getModelDisplay());
        resp.setTemperature(config.getTemperature() == null ? null : config.getTemperature().doubleValue());
        resp.setMaxTokens(config.getMaxTokens());
        resp.setThinkingEnabled(config.getThinkingEnabled() != null && config.getThinkingEnabled() == 1);
        resp.setTimeoutMs(config.getTimeoutMs());
        resp.setExtraBody(config.getExtraBody());
        resp.setTestStatus(config.getTestStatus());
        resp.setLastTestedAt(config.getLastTestedAt());
        resp.setLastTestLatencyMs(config.getLastTestLatencyMs());
        resp.setLastError(config.getLastError());
        resp.setIsDefault(config.getIsDefault() != null && config.getIsDefault() == 1);
        resp.setIsEnabled(config.getIsEnabled() != null && config.getIsEnabled() == 1);
        // 只回显掩码，永不回显明文（08 §7 S2）
        resp.setApiKeyHint(credentialService.hint(config.getCredentialRef()));
        resp.setHasCredential(config.getCredentialRef() != null);
        return resp;
    }

    private String normalizeJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        String trimmed = json.trim();
        return trimmed.length() > 2000 ? trimmed.substring(0, 2000) : trimmed;
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 255 ? value : value.substring(0, 255);
    }
}
