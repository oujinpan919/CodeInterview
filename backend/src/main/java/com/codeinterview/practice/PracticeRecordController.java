package com.codeinterview.practice;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codeinterview.common.result.PageResp;
import com.codeinterview.common.result.Result;
import com.codeinterview.practice.vo.PracticeRecordResp;

import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 刷题记录（04 A2 #15）。T09 只做记录列表，统计图表在 T11 补齐。
 */
@Tag(name = "刷题记录")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PracticeRecordController {

    private final PracticeRecordService practiceRecordService;

    @Operation(summary = "刷题记录（可按 selfAssess 筛选，按时间倒序）")
    @GetMapping("/practice-records")
    public Result<PageResp<PracticeRecordResp>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Integer selfAssess) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(practiceRecordService.page(userId, page, pageSize, selfAssess));
    }
}
