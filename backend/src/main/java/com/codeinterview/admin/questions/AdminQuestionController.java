package com.codeinterview.admin.questions;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codeinterview.admin.dto.QuestionSaveReq;
import com.codeinterview.admin.vo.AdminQuestionDetailResp;
import com.codeinterview.admin.vo.AdminQuestionListItemResp;
import com.codeinterview.admin.vo.QuestionIdResp;
import com.codeinterview.admin.vo.QuestionStatusResp;
import com.codeinterview.common.result.PageResp;
import com.codeinterview.common.result.Result;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 管理端题目维护（04 A3.9 #27–#33）。
 * 02 §5.4：`/admin/**` 拦截器校验 role=1 + 方法级 @SaCheckRole 双保险。
 */
@Tag(name = "管理端-题目")
@RestController
@RequestMapping("/api/v1/admin/questions")
@RequiredArgsConstructor
@SaCheckRole("ADMIN")
public class AdminQuestionController {

    private final AdminQuestionService adminQuestionService;

    @Operation(summary = "全状态题目列表（status/categoryId/keyword 筛选，分页）")
    @GetMapping
    public Result<PageResp<AdminQuestionListItemResp>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return Result.success(adminQuestionService.page(status, categoryId, keyword, page, pageSize));
    }

    @Operation(summary = "题目编辑详情（含 contentMd/answerMd/tagIds/source/status）")
    @GetMapping("/{id}")
    public Result<AdminQuestionDetailResp> detail(@PathVariable Long id) {
        return Result.success(adminQuestionService.detail(id));
    }

    @Operation(summary = "新建题目（初始 status=0 草稿）")
    @PostMapping
    public Result<QuestionIdResp> create(@RequestBody @Valid QuestionSaveReq req) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(new QuestionIdResp(adminQuestionService.create(req, userId)));
    }

    @Operation(summary = "更新题目（全量；已发布题保存后前台立即生效）")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid QuestionSaveReq req) {
        adminQuestionService.update(id, req);
        return Result.success();
    }

    @Operation(summary = "逻辑删除（deleted=1；不做引用检查）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminQuestionService.delete(id);
        return Result.success();
    }

    @Operation(summary = "发布（校验必填齐全；首次写 published_at，重新发布不覆盖）")
    @PutMapping("/{id}/publish")
    public Result<QuestionStatusResp> publish(@PathVariable Long id) {
        return Result.success(new QuestionStatusResp(adminQuestionService.publish(id)));
    }

    @Operation(summary = "下架（status=2；前台详情 40400，收藏/错题置灰）")
    @PutMapping("/{id}/offline")
    public Result<QuestionStatusResp> offline(@PathVariable Long id) {
        return Result.success(new QuestionStatusResp(adminQuestionService.offline(id)));
    }
}
