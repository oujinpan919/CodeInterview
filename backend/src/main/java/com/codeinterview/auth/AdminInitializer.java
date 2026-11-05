package com.codeinterview.auth;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.codeinterview.user.UserMapper;
import com.codeinterview.user.entity.User;

import cn.hutool.crypto.digest.BCrypt;
import lombok.extern.slf4j.Slf4j;

/**
 * 首次启动创建管理员（03 §6）：库中不存在 role=1 用户且配置了 admin.init-password 时建号，
 * 昵称固定 admin；强度校验按 profile 分级（02 §6.2）——local/dev/mock 弱密码只 WARN 照建，
 * prod 不合格拒绝建号。数据库不可用时只打日志，不影响应用启动（T02 阶段 /health 需能返回 DOWN）。
 */
@Slf4j
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final String ADMIN_NICKNAME = "admin";

    private final UserMapper userMapper;
    private final Environment environment;
    private final String initPassword;

    public AdminInitializer(UserMapper userMapper,
                            Environment environment,
                            @Value("${admin.init-password:}") String initPassword) {
        this.userMapper = userMapper;
        this.environment = environment;
        this.initPassword = initPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (userMapper.countAdmins() > 0) {
                log.info("admin already exists, skip initializing");
                return;
            }
            if (initPassword == null || initPassword.isBlank()) {
                log.warn("admin init-password is not configured, skip creating admin");
                return;
            }
            String policyError = PasswordPolicy.validate(initPassword, ADMIN_NICKNAME);
            if (policyError != null) {
                if (isProdProfile()) {
                    log.error("admin init password rejected by policy, skip creating admin");
                    return;
                }
                log.warn("admin init password is weak (profile={}), created anyway — change it before exposing",
                        currentProfile());
            }
            User admin = new User();
            admin.setNickname(ADMIN_NICKNAME);
            admin.setPassword(BCrypt.hashpw(initPassword, BCrypt.gensalt(12)));
            admin.setRole(User.ROLE_ADMIN);
            admin.setStatus(User.STATUS_NORMAL);
            admin.setDeleted(User.NOT_DELETED);
            userMapper.insert(admin);
            log.info("admin account created, nickname={}, id={}", ADMIN_NICKNAME, admin.getId());
        } catch (Exception ex) {
            log.warn("admin initializer skipped: {}", ex.getMessage());
        }
    }

    private boolean isProdProfile() {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equalsIgnoreCase);
    }

    private String currentProfile() {
        String[] active = environment.getActiveProfiles();
        if (active.length > 0) {
            return String.join(",", active);
        }
        String[] defaults = environment.getDefaultProfiles();
        return defaults.length > 0 ? defaults[0] : "default";
    }
}
