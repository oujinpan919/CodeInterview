package com.codeinterview.admin.taxonomy;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codeinterview.admin.dto.TagSaveReq;
import com.codeinterview.admin.vo.IdResp;
import com.codeinterview.common.result.Result;

import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 管理端标签维护（04 A3.9 #39–#41）。标签列表仍用公开 GET /tags（04 B4.12）。
 */
@Tag(name = "管理端-标签")
@RestController
@RequestMapping("/api/v1/admin/tags")
@RequiredArgsConstructor
@SaCheckRole("ADMIN")
public class AdminTagController {

    private final AdminTaxonomyService adminTaxonomyService;

    @Operation(summary = "新建标签")
    @PostMapping
    public Result<IdResp> create(@RequestBody @Valid TagSaveReq req) {
        return Result.success(new IdResp(adminTaxonomyService.createTag(req)));
    }

    @Operation(summary = "更新标签")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid TagSaveReq req) {
        adminTaxonomyService.updateTag(id, req);
        return Result.success();
    }

    @Operation(summary = "删除标签（被题目引用时 40901）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminTaxonomyService.deleteTag(id);
        return Result.success();
    }
}
