package com.codeinterview.review;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codeinterview.common.result.Result;
import com.codeinterview.review.vo.ReviewSummaryResp;

import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 今日计划（04 A2 #22–#23）。userId 只从登录态取，接口不接收 userId。
 */
@Tag(name = "今日计划")
@RestController
@RequestMapping("/api/v1/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "今日计划摘要（待复习 / 逾期 / 新题 / 估算分钟）")
    @GetMapping("/summary")
    public Result<ReviewSummaryResp> summary() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(reviewService.summary(userId));
    }
}
