package com.codeinterview.auth;

import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.codeinterview.auth.dto.LoginReq;
import com.codeinterview.auth.dto.PasswordUpdateReq;
import com.codeinterview.auth.dto.ProfileUpdateReq;
import com.codeinterview.auth.dto.RegisterReq;
import com.codeinterview.auth.vo.LoginResp;
import com.codeinterview.auth.vo.RegisterResp;
import com.codeinterview.auth.vo.UserResp;
import com.codeinterview.common.exception.BusinessException;
import com.codeinterview.common.result.ResultCode;
import com.codeinterview.user.UserService;
import com.codeinterview.user.entity.User;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 认证与账号（04 A3.1 / 02 §5.4.1）。
 * 昵称即登录名；密码 BCrypt cost=12；失败锁定按 IP+昵称；改密/改名后 kickout。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[\\u4e00-\\u9fa5A-Za-z0-9_-]{2,20}$");
    /** 账号不存在时也跑一次 BCrypt 校验，避免时序枚举（02 §5.4.1）。 */
    private static final String DUMMY_HASH = BCrypt.hashpw("codeinterview-dummy", BCrypt.gensalt(12));

    private final UserService userService;
    private final LoginAttemptGuard loginAttemptGuard;

    @Value("${app.register-enabled:true}")
    private boolean registerEnabled;

    public RegisterResp register(RegisterReq req, String ip) {
        if (!registerEnabled) {
            throw new BusinessException(ResultCode.FORBIDDEN, "注册已关闭，请联系管理员");
        }
        String nickname = req.getNickname() == null ? "" : req.getNickname().trim();
        if (!NICKNAME_PATTERN.matcher(nickname).matches()) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "昵称需为 2–20 个字符，可用中文、字母、数字、下划线、连字符");
        }
        String policyError = PasswordPolicy.validate(req.getPassword(), nickname);
        if (policyError != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, policyError);
        }
        if (!loginAttemptGuard.tryRegister(ip)) {
            throw new BusinessException(ResultCode.RATE_LIMITED, "操作太频繁，请稍后再试");
        }
        if (userService.findByNickname(nickname) != null) {
            throw new BusinessException(ResultCode.CONFLICT, "该昵称不可用，请换一个");
        }
        User user = userService.create(nickname, req.getPassword(), User.ROLE_NORMAL);
        log.info("register ok nickname={} userId={}", nickname, user.getId());
        return new RegisterResp(user.getId());
    }

    public LoginResp login(LoginReq req, String ip) {
        String nickname = req.getNickname() == null ? "" : req.getNickname().trim();
        if (nickname.isEmpty() || req.getPassword() == null || req.getPassword().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "昵称和密码不能为空");
        }
        if (loginAttemptGuard.isLocked(ip, nickname)) {
            log.warn("login locked ip={} nickname={}", ip, nickname);
            throw new BusinessException(ResultCode.LOGIN_LOCKED, "尝试次数过多，请 10 分钟后再试");
        }
        User user = userService.findByNickname(nickname);
        boolean matched = false;
        if (user == null) {
            BCrypt.checkpw(req.getPassword(), DUMMY_HASH);
        } else {
            matched = BCrypt.checkpw(req.getPassword(), user.getPassword());
        }
        if (user == null || !matched || (user.getDeleted() != null && user.getDeleted() == User.DELETED)) {
            loginAttemptGuard.recordFailure(ip, nickname);
            log.warn("login failed nickname={} ip={}", nickname, ip);
            throw new BusinessException(ResultCode.LOGIN_FAILED, "昵称或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == User.STATUS_DISABLED) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED, "账号已被禁用，请联系管理员");
        }
        loginAttemptGuard.clear(ip, nickname);
        StpUtil.login(user.getId());
        userService.updateLastLogin(user.getId());
        log.info("login ok nickname={} userId={}", nickname, user.getId());
        return new LoginResp(StpUtil.getTokenValue(), userService.toResp(user));
    }

    public void logout() {
        StpUtil.logout();
    }

    public UserResp me() {
        User user = currentUserOrThrow();
        return userService.toResp(user);
    }

    public UserResp updateProfile(ProfileUpdateReq req) {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = currentUserOrThrow();
        if (!BCrypt.checkpw(req.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "密码不正确");
        }
        String nickname = req.getNickname() == null ? "" : req.getNickname().trim();
        if (!NICKNAME_PATTERN.matcher(nickname).matches()) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "昵称需为 2–20 个字符，可用中文、字母、数字、下划线、连字符");
        }
        User sameNickname = userService.findByNickname(nickname);
        if (sameNickname != null && !userId.equals(sameNickname.getId())) {
            throw new BusinessException(ResultCode.CONFLICT, "该昵称不可用，请换一个");
        }
        userService.updateNickname(userId, nickname);
        StpUtil.kickout(userId);
        log.info("profile nickname updated userId={}", userId);
        user.setNickname(nickname);
        return userService.toResp(user);
    }

    public void updatePassword(PasswordUpdateReq req) {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = currentUserOrThrow();
        if (!BCrypt.checkpw(req.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "原密码不正确");
        }
        if (req.getOldPassword().equals(req.getNewPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "新密码不能与旧密码相同");
        }
        String policyError = PasswordPolicy.validate(req.getNewPassword(), user.getNickname());
        if (policyError != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, policyError);
        }
        userService.updatePassword(userId, req.getNewPassword());
        StpUtil.kickout(userId);
        log.info("password updated userId={}", userId);
    }

    private User currentUserOrThrow() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userService.findById(userId);
        if (user == null || (user.getDeleted() != null && user.getDeleted() == User.DELETED)) {
            StpUtil.logout();
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return user;
    }
}
