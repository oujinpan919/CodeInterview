package com.codeinterview.practice;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codeinterview.common.result.PageResp;
import com.codeinterview.common.result.Result;
import com.codeinterview.practice.vo.WrongBookItemResp;
import com.codeinterview.practice.vo.WrongBookSummaryResp;
import com.codeinterview.practice.vo.WrongResolvedResp;

import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 错题本 / 复习条目（04 A2 #16–#19）。userId 只从登录态取。
 */
@Tag(name = "错题本")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WrongBookController {

    private final WrongBookService wrongBookService;

    @Operation(summary = "错题本列表（scope=learning/reviewing/mastered/reviewed/all；status=2 由前端置灰）")
    @GetMapping("/wrong-book")
    public Result<PageResp<WrongBookItemResp>> list(
            @RequestParam(required = false) Integer resolved,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer difficulty) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(wrongBookService.page(userId, resolved, scope, page, pageSize,
                categoryId, difficulty));
    }

    @Operation(summary = "三个视图角标（在学 / 待回顾 / 真掌握）")
    @GetMapping("/wrong-book/summary")
    public Result<WrongBookSummaryResp> summary() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(wrongBookService.summary(userId));
    }

    @Operation(summary = "标记掌握（幂等；无记录 40400，他人记录 40300）")
    @PutMapping("/wrong-book/{questionId}/resolved")
    public Result<WrongResolvedResp> markResolved(@PathVariable Long questionId) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(new WrongResolvedResp(wrongBookService.markResolved(userId, questionId)));
    }

    @Operation(summary = "彻底移除（物理删除；不存在也返回成功）")
    @DeleteMapping("/wrong-book/{questionId}")
    public Result<Void> remove(@PathVariable Long questionId) {
        Long userId = StpUtil.getLoginIdAsLong();
        wrongBookService.remove(userId, questionId);
        return Result.success();
    }
}
