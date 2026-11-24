package com.codeinterview.ai.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codeinterview.ai.dto.PromptTemplateSaveReq;
import com.codeinterview.ai.service.PromptTemplateService;
import com.codeinterview.ai.vo.PromptTemplateResp;
import com.codeinterview.common.exception.BusinessException;
import com.codeinterview.common.result.Result;
import com.codeinterview.common.result.ResultCode;

import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Prompt 模板管理（D11）。<b>仅管理员</b>可增删改；普通用户走 /ai/templates 只读列表。
 */
@Tag(name = "AI Prompt 模板（管理）")
@RestController
@RequestMapping("/api/v1/admin/ai/templates")
@RequiredArgsConstructor
public class AdminPromptTemplateController {

    private final PromptTemplateService templateService;

    @Operation(summary = "模板列表（含正文）")
    @GetMapping
    public Result<List<PromptTemplateResp>> list() {
        requireAdmin();
        return Result.success(templateService.listAll());
    }

    @Operation(summary = "新增模板")
    @PostMapping
    public Result<PromptTemplateResp> create(@Valid @RequestBody PromptTemplateSaveReq req) {
        requireAdmin();
        return Result.success(templateService.create(req));
    }

    @Operation(summary = "修改模板")
    @PutMapping("/{id}")
    public Result<PromptTemplateResp> update(@PathVariable Long id,
                                             @Valid @RequestBody PromptTemplateSaveReq req) {
        requireAdmin();
        return Result.success(templateService.update(id, req));
    }

    @Operation(summary = "删除模板（内置不可删，可停用）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        requireAdmin();
        templateService.delete(id);
        return Result.success(null);
    }

    private void requireAdmin() {
        if (!StpUtil.hasRole("ADMIN")) {
            throw new BusinessException(ResultCode.ADMIN_REQUIRED);
        }
    }
}
