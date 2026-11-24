package com.codeinterview.ai.controller;

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

import com.codeinterview.ai.dto.ConnectTestReq;
import com.codeinterview.ai.dto.ModelConfigSaveReq;
import com.codeinterview.ai.service.UserModelConfigService;
import com.codeinterview.ai.vo.ConnectTestResp;
import com.codeinterview.ai.vo.ModelConfigResp;
import com.codeinterview.common.result.Result;

import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 用户级模型配置（HC-3/4 / 08 §4.2）。所有接口按登录态 userId 过滤（08 §7 S3）。
 */
@Tag(name = "AI 模型配置（用户）")
@RestController
@RequestMapping("/api/v1/ai/config")
@RequiredArgsConstructor
public class AiConfigController {

    private final UserModelConfigService configService;

    @Operation(summary = "我的模型配置列表")
    @GetMapping("/models")
    public Result<List<ModelConfigResp>> list() {
        return Result.success(configService.list(StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "配置详情")
    @GetMapping("/models/{id}")
    public Result<ModelConfigResp> detail(@PathVariable Long id) {
        return Result.success(configService.detail(StpUtil.getLoginIdAsLong(), id));
    }

    @Operation(summary = "新增配置")
    @PostMapping("/models")
    public Result<ModelConfigResp> create(@Valid @RequestBody ModelConfigSaveReq req) {
        return Result.success(configService.create(StpUtil.getLoginIdAsLong(), req));
    }

    @Operation(summary = "修改配置（apiKey 留空表示不修改凭证）")
    @PutMapping("/models/{id}")
    public Result<ModelConfigResp> update(@PathVariable Long id, @Valid @RequestBody ModelConfigSaveReq req) {
        return Result.success(configService.update(StpUtil.getLoginIdAsLong(), id, req));
    }

    @Operation(summary = "删除配置（被会话引用时拒绝，需改为停用）")
    @DeleteMapping("/models/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        configService.delete(StpUtil.getLoginIdAsLong(), id);
        return Result.success(null);
    }

    @Operation(summary = "设为默认模型")
    @PutMapping("/models/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        configService.setDefault(StpUtil.getLoginIdAsLong(), id);
        return Result.success(null);
    }

    @Operation(summary = "启用 / 停用")
    @PutMapping("/models/{id}/enabled")
    public Result<Void> toggle(@PathVariable Long id, @RequestParam boolean enabled) {
        configService.toggle(StpUtil.getLoginIdAsLong(), id, enabled);
        return Result.success(null);
    }

    @Operation(summary = "连接测试（先 /models，不可用降级 chat）")
    @PostMapping("/test")
    public Result<ConnectTestResp> test(@Valid @RequestBody ConnectTestReq req) {
        return Result.success(configService.test(StpUtil.getLoginIdAsLong(), req));
    }
}
