package com.codeinterview.home;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codeinterview.common.result.Result;
import com.codeinterview.home.vo.HomeResp;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "首页")
@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @Operation(summary = "首页聚合（分类/最新/热门/继续刷；游客 continueLearning=null）")
    @GetMapping
    public Result<HomeResp> home() {
        return Result.success(homeService.home());
    }
}
