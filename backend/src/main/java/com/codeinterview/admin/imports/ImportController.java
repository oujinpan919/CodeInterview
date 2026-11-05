package com.codeinterview.admin.imports;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codeinterview.admin.dto.ImportReq;
import com.codeinterview.common.result.Result;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 批量导入入口（04 A2 #34 / 04 A3.9 #34）：POST /admin/questions/import。
 */
@Tag(name = "管理端-导入")
@RestController
@RequestMapping("/api/v1/admin/questions")
@RequiredArgsConstructor
@SaCheckRole("ADMIN")
public class ImportController {

    private final ImportService importService;

    @Operation(summary = "JSON 批量导入（逐条结果：created/skipped/failed + errors[index,reason]）")
    @PostMapping("/import")
    public Result<ImportResult> importJson(@RequestBody @Valid ImportReq req) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(importService.importJson(userId, req.getJson()));
    }
}
