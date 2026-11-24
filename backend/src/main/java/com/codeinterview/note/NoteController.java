package com.codeinterview.note;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codeinterview.common.result.Result;
import com.codeinterview.note.dto.NoteSaveReq;
import com.codeinterview.note.vo.NoteResp;

import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 题目笔记（04 C5 #48）。userId 只从登录态取。
 */
@Tag(name = "题目笔记")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @Operation(summary = "读我的笔记（未写过返回空串 + updatedAt=null）")
    @GetMapping("/questions/{questionId}/note")
    public Result<NoteResp> get(@PathVariable Long questionId) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(noteService.get(userId, questionId));
    }

    @Operation(summary = "保存我的笔记（空内容 = 删除；≤20000 字符）")
    @PutMapping("/questions/{questionId}/note")
    public Result<NoteResp> save(@PathVariable Long questionId, @RequestBody(required = false) NoteSaveReq req) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(noteService.save(userId, questionId, req));
    }
}
