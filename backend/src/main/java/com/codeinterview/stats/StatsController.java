package com.codeinterview.stats;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codeinterview.common.result.Result;
import com.codeinterview.stats.vo.CategoryStatResp;
import com.codeinterview.stats.vo.StatsOverviewResp;
import com.codeinterview.stats.vo.TrendPointResp;

import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 统计（04 A2 #19–#21）。userId 只从登录态取。
 */
@Tag(name = "统计")
@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @Operation(summary = "指标卡（今日/近7天/掌握率/连续天数/累计题数 + 累计次数）")
    @GetMapping("/overview")
    public Result<StatsOverviewResp> overview() {
        return Result.success(statsService.overview(StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "近 N 天自评趋势（days 仅 7/30，其他按 30；连续日期补 0）")
    @GetMapping("/trend")
    public Result<List<TrendPointResp>> trend(@RequestParam(required = false) Integer days) {
        return Result.success(statsService.trend(StpUtil.getLoginIdAsLong(), days));
    }

    @Operation(summary = "分类掌握分布（count 按自评条数聚合）")
    @GetMapping("/category")
    public Result<List<CategoryStatResp>> category() {
        return Result.success(statsService.category(StpUtil.getLoginIdAsLong()));
    }
}
