package com.codeinterview.admin.taxonomy;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codeinterview.admin.dto.CategorySaveReq;
import com.codeinterview.admin.vo.AdminCategoryResp;
import com.codeinterview.admin.vo.IdResp;
import com.codeinterview.common.result.Result;

import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 管理端分类维护（04 A3.9 #35–#38）。
 * 与公开 GET /categories 口径不同：这里含停用分类，questionCount 按未删除题计（要点 5）。
 */
@Tag(name = "管理端-分类")
@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
@SaCheckRole("ADMIN")
public class AdminCategoryController {

    private final AdminTaxonomyService adminTaxonomyService;

    @Operation(summary = "全部分类（含停用；questionCount=未删除题数，publishedCount=发布态）")
    @GetMapping
    public Result<List<AdminCategoryResp>> list() {
        return Result.success(adminTaxonomyService.categories());
    }

    @Operation(summary = "新建分类")
    @PostMapping
    public Result<IdResp> create(@RequestBody @Valid CategorySaveReq req) {
        return Result.success(new IdResp(adminTaxonomyService.createCategory(req)));
    }

    @Operation(summary = "更新分类（全量）")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid CategorySaveReq req) {
        adminTaxonomyService.updateCategory(id, req);
        return Result.success();
    }

    @Operation(summary = "删除分类（被未删除题引用时 40901）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminTaxonomyService.deleteCategory(id);
        return Result.success();
    }
}
