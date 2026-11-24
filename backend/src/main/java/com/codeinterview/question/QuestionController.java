package com.codeinterview.question;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codeinterview.common.result.PageResp;
import com.codeinterview.common.result.Result;
import com.codeinterview.question.dto.QuestionQueryReq;
import com.codeinterview.question.vo.NeighborResp;
import com.codeinterview.question.vo.QuestionBriefResp;
import com.codeinterview.question.vo.QuestionDetailResp;
import com.codeinterview.question.vo.RandomQuestionResp;
import com.codeinterview.question.vo.RecentViewResp;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "题目")
@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @Operation(summary = "题目列表（分类/标签/难度/关键词筛选，latest|hot 排序，分页）")
    @GetMapping
    public Result<PageResp<QuestionBriefResp>> list(QuestionQueryReq query) {
        return Result.success(questionService.page(query));
    }

    @Operation(summary = "随机取题（发布态池随机 1 道，可带筛选、可排除已出题；空池 data=null）")
    // 显式声明精确路径：Spring 精确匹配优先于 /{id} 路径变量，否则 "random" 会被当 Long 转换报 40000
    @GetMapping("/random")
    public Result<RandomQuestionResp> random(QuestionQueryReq query,
                                             @RequestParam(required = false) String excludeIds) {
        return Result.success(questionService.random(query, excludeIds));
    }

    @Operation(summary = "题目详情（含收藏态/我的自评/上下题；ctx=list|favorite|wrong 时上下题按来源列表顺序）")
    @GetMapping("/{id}")
    public Result<QuestionDetailResp> detail(@PathVariable Long id,
                                             @RequestParam(required = false) String ctx,
                                             @RequestParam(required = false) String scope,
                                             QuestionQueryReq query) {
        return Result.success(questionService.detail(id, ctx, scope, query));
    }

    @Operation(summary = "最近浏览的题目（题目练习页左侧列表；未登录返回空数组）")
    @GetMapping("/recent-views")
    public Result<List<RecentViewResp>> recentViews(@RequestParam(required = false) Integer limit,
                                                    @RequestParam(required = false) Long categoryId) {
        return Result.success(questionService.recentViews(limit, categoryId));
    }

    @Operation(summary = "上/下一题（同分类按 id，边界 null，不循环）")
    @GetMapping("/{id}/neighbors")
    public Result<NeighborResp> neighbors(@PathVariable Long id) {
        return Result.success(questionService.neighbors(id));
    }

    @Operation(summary = "相似题（同分类优先 + 共享标签，默认 3 条；v3.6 C10）")
    @GetMapping("/{id}/similar")
    public Result<List<QuestionBriefResp>> similar(@PathVariable Long id,
                                                  @RequestParam(required = false) Integer limit) {
        return Result.success(questionService.similar(id, limit));
    }
}
