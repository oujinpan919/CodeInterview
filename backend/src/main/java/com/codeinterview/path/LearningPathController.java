package com.codeinterview.path;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codeinterview.common.result.Result;
import com.codeinterview.path.vo.PathDetailResp;
import com.codeinterview.path.vo.PathResp;

import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 刷题路线（04 C8 #51–#52）。公开只读；登录时额外返回本人进度。
 */
@Slf4j
@Tag(name = "刷题路线")
@RestController
@RequestMapping("/api/v1/learning-paths")
@RequiredArgsConstructor
public class LearningPathController {

    private final LearningPathService learningPathService;

    @Operation(summary = "路线列表（登录返回本人已完成题数，游客 completedCount=null）")
    @GetMapping
    public Result<List<PathResp>> list() {
        return Result.success(learningPathService.list(currentUserIdOrNull()));
    }

    @Operation(summary = "路线详情（有序题目 + 完成标记）")
    @GetMapping("/{id}")
    public Result<PathDetailResp> detail(@PathVariable Long id) {
        return Result.success(learningPathService.detail(id, currentUserIdOrNull()));
    }

    /** 需要「可能登录也可能游客」的场景：登录态读取失败按游客处理，不影响只读接口。 */
    private Long currentUserIdOrNull() {
        try {
            return StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        } catch (Exception ex) {
            log.debug("read login state failed: {}", ex.getMessage());
            return null;
        }
    }
}
