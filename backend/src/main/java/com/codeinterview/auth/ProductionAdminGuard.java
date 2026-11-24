package com.codeinterview.auth;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.codeinterview.user.UserMapper;
import com.codeinterview.user.entity.User;

import cn.hutool.crypto.digest.BCrypt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 生产启动安全闸：拒绝带演示口令的管理员/演示账号进入生产。
 *
 * <p>背景：db/seed-demo.sql 会创建 admin/123456 与 demo/123456。该脚本只用于本机演示，
 * 但一旦被误用于生产，已知口令即可接管后台。这里在 prod profile 下做启动阻断，
 * 迫使运维先改密/清库再启动，而不是仅打印一条容易被忽略的 WARN。
 *
 * <p>数据库不可用时只记录 WARN，不阻断启动（与 /health 的 DOWN 语义一致）。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class ProductionAdminGuard implements ApplicationRunner {

    private static final String DEMO_PASSWORD = "123456";
    private static final String DEMO_NICKNAME = "demo";

    private final UserMapper userMapper;
    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        if (!isProdProfile()) {
            return;
        }
        try {
            for (User admin : userMapper.selectAdmins()) {
                if (admin.getPassword() != null && isDemoPassword(admin.getPassword())) {
                    throw new IllegalStateException(
                            "生产环境检测到管理员仍使用演示口令 123456。"
                                    + "请先在 local/维护环境修改管理员密码，再以 prod 启动。");
                }
            }
            User demo = userMapper.selectByNicknameIncludingDeleted(DEMO_NICKNAME);
            if (demo != null && (demo.getDeleted() == null || demo.getDeleted() == User.NOT_DELETED)) {
                throw new IllegalStateException(
                        "生产环境检测到 demo 演示账号。请删除该账号或改用非 seed-demo 的生产初始化脚本。");
            }
            log.info("[security] production admin/demo credential check passed");
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("[security] production credential check skipped: {}", ex.getMessage());
        }
    }

    private boolean isDemoPassword(String hash) {
        try {
            return BCrypt.checkpw(DEMO_PASSWORD, hash);
        } catch (RuntimeException ex) {
            log.warn("[security] invalid admin password hash, skip check: {}", ex.getMessage());
            return false;
        }
    }

    private boolean isProdProfile() {
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }
}
