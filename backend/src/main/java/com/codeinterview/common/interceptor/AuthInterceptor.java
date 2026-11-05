package com.codeinterview.common.interceptor;

import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.codeinterview.common.exception.BusinessException;
import com.codeinterview.common.result.ResultCode;
import com.codeinterview.user.UserMapper;
import com.codeinterview.user.entity.User;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 登录态 + 角色拦截（02 §5.4）。白名单按 02 §5.4：
 * /auth/login、/auth/register、GET questions/categories/tags/home、/health、swagger、/error。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/health",
            "/error");

    /** 公开只读 GET（含详情与上下题）。 */
    private static final Pattern PUBLIC_GET_PATTERN =
            Pattern.compile("^/api/v1/(home|categories|tags|questions(/\\d+)?(/neighbors)?)$");

    private final UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/v1/")) {
            return true;
        }
        if (PUBLIC_PATHS.contains(path)) {
            return true;
        }
        if ("GET".equalsIgnoreCase(request.getMethod()) && PUBLIC_GET_PATTERN.matcher(path).matches()) {
            return true;
        }
        try {
            StpUtil.checkLogin();
        } catch (NotLoginException e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectByIdIncludingDeleted(userId);
        if (user == null || (user.getDeleted() != null && user.getDeleted() == User.DELETED)) {
            StpUtil.logout();
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (user.getStatus() != null && user.getStatus() == User.STATUS_DISABLED) {
            StpUtil.logout();
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED, "账号已被禁用，请联系管理员");
        }
        if (path.startsWith("/api/v1/admin/")) {
            try {
                StpUtil.checkRole("ADMIN");
            } catch (NotRoleException e) {
                throw new BusinessException(ResultCode.ADMIN_REQUIRED);
            }
        }
        return true;
    }
}
