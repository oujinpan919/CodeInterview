package com.codeinterview.practice;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codeinterview.common.result.Result;
import com.codeinterview.practice.dto.AssessmentReq;
import com.codeinterview.practice.vo.AssessmentResp;

import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 三档自评（04 A2 #14）。userId 只从登录态取。
 */
@Tag(name = "自评")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;

    @Operation(summary = "三档自评（追加 practice_record + 维护错题本 + practice_count+1，同一事务）")
    @PostMapping("/questions/{id}/assessment")
    public Result<AssessmentResp> assess(@PathVariable Long id, @RequestBody @Valid AssessmentReq req) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(assessmentService.assess(userId, id, req));
    }
}
