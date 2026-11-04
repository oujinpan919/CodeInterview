package com.codeinterview.auth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codeinterview.auth.dto.LoginReq;
import com.codeinterview.auth.dto.PasswordUpdateReq;
import com.codeinterview.auth.dto.ProfileUpdateReq;
import com.codeinterview.auth.dto.RegisterReq;
import com.codeinterview.auth.vo.LoginResp;
import com.codeinterview.auth.vo.RegisterResp;
import com.codeinterview.auth.vo.UserResp;
import com.codeinterview.common.result.Result;
import com.codeinterview.common.util.IpUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "注册（不自动登录）")
    @PostMapping("/register")
    public Result<RegisterResp> register(@RequestBody @Valid RegisterReq req, HttpServletRequest request) {
        return Result.success(authService.register(req, IpUtil.getClientIp(request)));
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<LoginResp> login(@RequestBody @Valid LoginReq req, HttpServletRequest request) {
        return Result.success(authService.login(req, IpUtil.getClientIp(request)));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    @Operation(summary = "当前用户")
    @GetMapping("/me")
    public Result<UserResp> me() {
        return Result.success(authService.me());
    }

    @Operation(summary = "修改昵称（成功后需重新登录）")
    @PutMapping("/profile")
    public Result<UserResp> profile(@RequestBody @Valid ProfileUpdateReq req) {
        return Result.success(authService.updateProfile(req));
    }

    @Operation(summary = "修改密码（成功后需重新登录）")
    @PutMapping("/password")
    public Result<Void> password(@RequestBody @Valid PasswordUpdateReq req) {
        authService.updatePassword(req);
        return Result.success();
    }
}
