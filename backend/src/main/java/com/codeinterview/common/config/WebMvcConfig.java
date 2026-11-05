package com.codeinterview.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.codeinterview.common.interceptor.RateLimitInterceptor;

import lombok.RequiredArgsConstructor;

/**
 * 拦截器装配（06 T17）：普通读接口限流独立于 SaTokenConfig 的鉴权拦截器，
 * 只对 /api/v1/** 的 GET 生效；/health 供探针高频访问，不参与限流。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns("/api/v1/health");
    }
}
