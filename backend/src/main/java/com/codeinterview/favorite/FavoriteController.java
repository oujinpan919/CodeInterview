package com.codeinterview.favorite;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codeinterview.common.result.PageResp;
import com.codeinterview.common.result.Result;
import com.codeinterview.favorite.vo.FavoriteQuestionResp;
import com.codeinterview.favorite.vo.FavoriteStateResp;

import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 收藏（04 A2 #11–#13）。userId 只从登录态取，接口不接收 userId（01 §4.4）。
 */
@Tag(name = "收藏")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "收藏（幂等；重复收藏仍返回 favorited=true）")
    @PostMapping("/questions/{id}/favorite")
    public Result<FavoriteStateResp> favorite(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(new FavoriteStateResp(favoriteService.favorite(userId, id)));
    }

    @Operation(summary = "取消收藏（物理删除；不存在也返回成功）")
    @DeleteMapping("/questions/{id}/favorite")
    public Result<FavoriteStateResp> unfavorite(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(new FavoriteStateResp(favoriteService.unfavorite(userId, id)));
    }

    @Operation(summary = "我的收藏（按收藏时间倒序；status=2 由前端置灰）")
    @GetMapping("/favorites")
    public Result<PageResp<FavoriteQuestionResp>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(favoriteService.page(userId, page, pageSize));
    }
}
