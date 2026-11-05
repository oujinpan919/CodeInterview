package com.codeinterview.common.config;

import java.util.Collections;
import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.codeinterview.common.interceptor.AuthInterceptor;
import com.codeinterview.user.UserMapper;
import com.codeinterview.user.entity.User;

import cn.dev33.satoken.stp.StpInterface;
import lombok.RequiredArgsConstructor;

/**
 * Sa-Token 接入（02 §5.4）：/api/v1/** 默认拦截，登录/注册/health 与公开只读 GET 放行，
 * 管理端二次校验 role=1（拦截器 + StpInterface 双层，避免只靠前端隐藏菜单）。
 */
@Configuration("saTokenWebMvcConfig")
@RequiredArgsConstructor
public class SaTokenConfig implements WebMvcConfigurer, StpInterface {

    private final AuthInterceptor authInterceptor;
    private final UserMapper userMapper;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns(
                        "/api/v1/auth/login",
                        "/api/v1/auth/register",
                        "/api/v1/health");
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (loginId == null) {
            return Collections.emptyList();
        }
        User user = userMapper.selectByIdIncludingDeleted(Long.valueOf(String.valueOf(loginId)));
        if (user == null || user.getDeleted() == null || user.getDeleted() == User.DELETED) {
            return Collections.emptyList();
        }
        return Integer.valueOf(User.ROLE_ADMIN).equals(user.getRole()) ? List.of("ADMIN") : Collections.emptyList();
    }
}
