package com.codeinterview.ai.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codeinterview.ai.config.ConnectionTester;
import com.codeinterview.ai.config.ModelConfigResolver;
import com.codeinterview.ai.config.OutboundUrlGuard;
import com.codeinterview.ai.dto.ConnectTestReq;
import com.codeinterview.ai.dto.ModelConfigSaveReq;
import com.codeinterview.ai.entity.AiConfigAudit;
import com.codeinterview.ai.entity.AiCredential;
import com.codeinterview.ai.entity.SiteModelConfig;
import com.codeinterview.ai.mapper.AiConfigAuditMapper;
import com.codeinterview.ai.mapper.SiteModelConfigMapper;
import com.codeinterview.ai.service.CredentialService;
import com.codeinterview.ai.vo.ConnectTestResp;
import com.codeinterview.ai.vo.ModelConfigResp;
import com.codeinterview.ai.vo.SiteSettingResp;
import com.codeinterview.common.exception.BusinessException;
import com.codeinterview.common.result.Result;
import com.codeinterview.common.result.ResultCode;

import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 站点级模型配置与站点开关（D2：管理员维护，可多条，至多 1 条 is_site_default）。
 * 仅 role=1 可访问。
 */
@Tag(name = "AI 站点配置（管理）")
@RestController
@RequestMapping("/api/v1/admin/ai")
@RequiredArgsConstructor
public class AdminAiConfigController {

    private final SiteModelConfigMapper siteConfigMapper;
    private final AiConfigAuditMapper auditMapper;
    private final CredentialService credentialService;
    private final OutboundUrlGuard urlGuard;
    private final ConnectionTester connectionTester;
    private final ModelConfigResolver resolver;

    @Operation(summary = "站点级模型配置列表")
    @GetMapping("/models")
    public Result<List<ModelConfigResp>> list() {
        requireAdmin();
        List<SiteModelConfig> rows = siteConfigMapper.selectList(null);
        List<ModelConfigResp> result = new ArrayList<>(rows.size());
        for (SiteModelConfig row : rows) {
            if (row.getDeleted() != null && row.getDeleted() == 1) {
                continue;
            }
            result.add(toResp(row));
        }
        return Result.success(result);
    }

    @Operation(summary = "新增站点级配置")
    @PostMapping("/models")
    public Result<ModelConfigResp> create(@Valid @RequestBody ModelConfigSaveReq req) {
        requireAdmin();
        String normalized = urlGuard.validate(req.getBaseUrl()).normalizedUrl();

        SiteModelConfig config = new SiteModelConfig();
        applyFields(config, req, normalized);
        config.setIsEnabled(req.getIsEnabled() == null || req.getIsEnabled() ? 1 : 0);
        config.setIsSiteDefault(0);
        config.setDeleted(0);
        config.setTestStatus("untested");
        if (req.getApiKey() != null && !req.getApiKey().isBlank()) {
            config.setCredentialRef(credentialService.save(null, AiCredential.OWNER_SITE, 0L, req.getApiKey().trim()));
        }
        siteConfigMapper.insert(config);

        if (Boolean.TRUE.equals(req.getMakeDefault())) {
            siteConfigMapper.clearDefault();
            siteConfigMapper.setDefault(config.getId());
        }
        audit(AiConfigAudit.ACTION_CREATE, config.getId(), req.getDisplayName());
        return Result.success(toResp(siteConfigMapper.selectById(config.getId())));
    }

    @Operation(summary = "修改站点级配置")
    @PutMapping("/models/{id}")
    public Result<ModelConfigResp> update(@PathVariable Long id, @Valid @RequestBody ModelConfigSaveReq req) {
        requireAdmin();
        SiteModelConfig existing = requireExisting(id);
        String normalized = urlGuard.validate(req.getBaseUrl()).normalizedUrl();
        applyFields(existing, req, normalized);
        if (req.getIsEnabled() != null) {
            existing.setIsEnabled(req.getIsEnabled() ? 1 : 0);
            if (!req.getIsEnabled()) {
                existing.setIsSiteDefault(0);
            }
        }
        if (req.getApiKey() != null && !req.getApiKey().isBlank()) {
            existing.setCredentialRef(credentialService.save(existing.getCredentialRef(),
                    AiCredential.OWNER_SITE, 0L, req.getApiKey().trim()));
            existing.setTestStatus("untested");
        }
        siteConfigMapper.updateById(existing);

        if (Boolean.TRUE.equals(req.getMakeDefault()) && existing.getIsEnabled() == 1) {
            siteConfigMapper.clearDefault();
            siteConfigMapper.setDefault(id);
        }
        audit(AiConfigAudit.ACTION_UPDATE, id, req.getDisplayName());
        return Result.success(toResp(siteConfigMapper.selectById(id)));
    }

    @Operation(summary = "删除站点级配置（软删）")
    @DeleteMapping("/models/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        requireAdmin();
        SiteModelConfig existing = requireExisting(id);
        existing.setDeleted(1);
        existing.setIsEnabled(0);
        existing.setIsSiteDefault(0);
        siteConfigMapper.updateById(existing);
        credentialService.delete(existing.getCredentialRef());
        audit(AiConfigAudit.ACTION_DELETE, id, existing.getDisplayName());
        return Result.success(null);
    }

    @Operation(summary = "设为站点默认")
    @PutMapping("/models/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        requireAdmin();
        SiteModelConfig existing = requireExisting(id);
        if (existing.getIsEnabled() != 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请先启用该配置");
        }
        siteConfigMapper.clearDefault();
        siteConfigMapper.setDefault(id);
        audit(AiConfigAudit.ACTION_SET_DEFAULT, id, existing.getDisplayName());
        return Result.success(null);
    }

    @Operation(summary = "连接测试（站点级，消耗站点 Key）")
    @PostMapping("/test")
    public Result<ConnectTestResp> test(@Valid @RequestBody ConnectTestReq req) {
        requireAdmin();
        String resolvedKey = null;
        if (req.getConfigId() != null) {
            SiteModelConfig existing = siteConfigMapper.selectById(req.getConfigId());
            if (existing != null) {
                resolvedKey = credentialService.plaintext(existing.getCredentialRef());
            }
        }
        ConnectionTester.Result result = connectionTester.test(req, resolvedKey);
        if (req.getConfigId() != null) {
            SiteModelConfig existing = siteConfigMapper.selectById(req.getConfigId());
            if (existing != null) {
                existing.setTestStatus(result.statusId());
                existing.setLastTestedAt(java.time.LocalDateTime.now());
                existing.setLastTestLatencyMs((int) result.latencyMs());
                existing.setLastError(result.ok() ? null : truncate(result.error()));
                siteConfigMapper.updateById(existing);
            }
        }
        ConnectTestResp resp = new ConnectTestResp();
        resp.setStatus(result.statusId());
        resp.setOk(result.ok());
        resp.setLatencyMs(result.latencyMs());
        resp.setModels(result.models());
        resp.setSuggestV1(result.suggestV1());
        resp.setError(result.error());
        return Result.success(resp);
    }

    // ---- 站点开关（D2）----

    @Operation(summary = "读取站点开关")
    @GetMapping("/settings")
    public Result<SiteSettingResp> settings() {
        requireAdmin();
        SiteSettingResp resp = new SiteSettingResp();
        resp.setAllowUserOwnKey(resolver.allowUserOwnKey());
        return Result.success(resp);
    }

    @Operation(summary = "修改站点开关（关闭后用户自带 Key 不生效，但数据保留）")
    @PutMapping("/settings")
    public Result<SiteSettingResp> updateSettings(@RequestParam boolean allowUserOwnKey) {
        requireAdmin();
        resolver.setAllowUserOwnKey(allowUserOwnKey);
        SiteSettingResp resp = new SiteSettingResp();
        resp.setAllowUserOwnKey(resolver.allowUserOwnKey());
        return Result.success(resp);
    }

    private void requireAdmin() {
        if (!StpUtil.hasRole("ADMIN")) {
            throw new BusinessException(ResultCode.ADMIN_REQUIRED);
        }
    }

    private SiteModelConfig requireExisting(Long id) {
        SiteModelConfig config = siteConfigMapper.selectById(id);
        if (config == null || (config.getDeleted() != null && config.getDeleted() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return config;
    }

    private void applyFields(SiteModelConfig config, ModelConfigSaveReq req, String normalizedUrl) {
        if (req.getDisplayName() == null || req.getDisplayName().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请填写展示名");
        }
        if (req.getModelId() == null || req.getModelId().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请填写模型 ID");
        }
        config.setProviderType(req.getProviderType() == null ? "custom" : req.getProviderType());
        config.setDisplayName(req.getDisplayName().trim());
        config.setBaseUrl(normalizedUrl);
        config.setAccessMode(req.getAccessMode() == null ? "direct" : req.getAccessMode());
        config.setModelId(req.getModelId().trim());
        config.setModelDisplay(req.getModelDisplay() == null || req.getModelDisplay().isBlank()
                ? req.getModelId().trim() : req.getModelDisplay().trim());
        config.setTemperature(req.getTemperature() == null
                ? new java.math.BigDecimal("0.30") : java.math.BigDecimal.valueOf(req.getTemperature()));
        config.setMaxTokens(req.getMaxTokens() == null ? 1200 : req.getMaxTokens());
        config.setThinkingEnabled(Boolean.TRUE.equals(req.getThinkingEnabled()) ? 1 : 0);
        config.setTimeoutMs(req.getTimeoutMs());
        config.setExtraBody(req.getExtraBody());
    }

    private void audit(String action, Long targetId, String detail) {
        AiConfigAudit entry = new AiConfigAudit();
        entry.setUserId(StpUtil.getLoginIdAsLong());
        entry.setTargetType(AiConfigAudit.TARGET_SITE);
        entry.setTargetId(targetId);
        entry.setAction(action);
        entry.setDetail(detail == null ? "" : truncate(detail));
        try {
            auditMapper.insert(entry);
        } catch (Exception ignored) {
            // 审计失败不影响主流程
        }
    }

    private ModelConfigResp toResp(SiteModelConfig config) {
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
        resp.setIsDefault(config.getIsSiteDefault() != null && config.getIsSiteDefault() == 1);
        resp.setIsEnabled(config.getIsEnabled() != null && config.getIsEnabled() == 1);
        resp.setApiKeyHint(credentialService.hint(config.getCredentialRef()));
        resp.setHasCredential(config.getCredentialRef() != null);
        return resp;
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 255 ? value : value.substring(0, 255);
    }
}
