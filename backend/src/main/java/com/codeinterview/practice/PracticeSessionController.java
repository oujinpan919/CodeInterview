package com.codeinterview.practice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codeinterview.common.result.Result;
import com.codeinterview.practice.dto.SessionAdvanceReq;
import com.codeinterview.practice.dto.SessionCreateReq;
import com.codeinterview.practice.vo.SessionProgressResp;

import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "练习会话")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PracticeSessionController {

    private final PracticeSessionService practiceSessionService;

    @Operation(summary = "创建/开始练习会话（登录态；已有进行中会话返回既有进度）")
    @PostMapping("/practice-sessions")
    public Result<SessionProgressResp> create(@RequestBody SessionCreateReq req) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(practiceSessionService.create(userId, req));
    }

    @Operation(summary = "读取当前进行中会话与进度（含 E-04 失效跳过）")
    @GetMapping("/practice-sessions/current")
    public Result<SessionProgressResp> current() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(practiceSessionService.current(userId));
    }

    @Operation(summary = "推进/结束会话（完成当前题 / 放弃）")
    @PostMapping("/practice-sessions/{id}/advance")
    public Result<SessionProgressResp> advance(@PathVariable Long id, @RequestBody SessionAdvanceReq req) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(practiceSessionService.advance(userId, id, req));
    }
}
