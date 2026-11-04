package com.codeinterview.question;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codeinterview.common.result.PageResp;
import com.codeinterview.common.result.Result;
import com.codeinterview.question.dto.QuestionQueryReq;
import com.codeinterview.question.vo.NeighborResp;
import com.codeinterview.question.vo.QuestionBriefResp;
import com.codeinterview.question.vo.QuestionDetailResp;

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

    @Operation(summary = "题目详情（含收藏态/我的自评/上下题；未登录 myFavorite=false、myAssessment=null）")
    @GetMapping("/{id}")
    public Result<QuestionDetailResp> detail(@PathVariable Long id) {
        return Result.success(questionService.detail(id));
    }

    @Operation(summary = "上/下一题（同分类按 id，边界 null，不循环）")
    @GetMapping("/{id}/neighbors")
    public Result<NeighborResp> neighbors(@PathVariable Long id) {
        return Result.success(questionService.neighbors(id));
    }
}
