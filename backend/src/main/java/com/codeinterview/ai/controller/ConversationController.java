package com.codeinterview.ai.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codeinterview.ai.service.ConversationService;
import com.codeinterview.ai.vo.ConversationDetailResp;
import com.codeinterview.ai.vo.ConversationResp;
import com.codeinterview.common.result.PageResp;
import com.codeinterview.common.result.Result;

import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * 会话历史（D6 / 08 §4.1 左侧会话栏）。所有接口按登录态 userId 过滤。
 */
@Tag(name = "AI 会话历史")
@RestController
@RequestMapping("/api/v1/ai/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @Operation(summary = "会话列表（时间倒序，分页）")
    @GetMapping
    public Result<PageResp<ConversationResp>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(conversationService.list(StpUtil.getLoginIdAsLong(), page, size));
    }

    @Operation(summary = "会话详情（含消息）")
    @GetMapping("/{id}")
    public Result<ConversationDetailResp> detail(@PathVariable Long id) {
        return Result.success(conversationService.detail(StpUtil.getLoginIdAsLong(), id));
    }

    @Operation(summary = "新建会话")
    @PostMapping
    public Result<Long> create(@RequestBody(required = false) CreateReq req) {
        Long questionId = req == null ? null : req.getQuestionId();
        Long modelConfigId = req == null ? null : req.getModelConfigId();
        return Result.success(conversationService.create(StpUtil.getLoginIdAsLong(), questionId, modelConfigId));
    }

    @Operation(summary = "会话改名")
    @PatchMapping("/{id}")
    public Result<Void> rename(@PathVariable Long id, @RequestBody RenameReq req) {
        conversationService.rename(StpUtil.getLoginIdAsLong(), id, req.getTitle());
        return Result.success(null);
    }

    @Operation(summary = "删除会话（软删）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        conversationService.delete(StpUtil.getLoginIdAsLong(), id);
        return Result.success(null);
    }

    @Data
    public static class CreateReq {
        private Long questionId;
        private Long modelConfigId;
    }

    @Data
    public static class RenameReq {
        @Size(max = 60)
        private String title;
    }
}
