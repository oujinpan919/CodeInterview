package com.codeinterview.user;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.codeinterview.auth.vo.UserResp;
import com.codeinterview.user.entity.User;

import cn.hutool.crypto.digest.BCrypt;
import lombok.RequiredArgsConstructor;

/**
 * 用户域读写（T04）。BCrypt cost 固定 12（02 §5.4.1）。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private static final int BCRYPT_COST = 12;

    private final UserMapper userMapper;

    /** 包含已逻辑删除行（昵称永不复用，登录时据此返回 40101）。 */
    public User findByNickname(String nickname) {
        return userMapper.selectByNicknameIncludingDeleted(nickname);
    }

    public User findById(Long id) {
        return userMapper.selectByIdIncludingDeleted(id);
    }

    public User create(String nickname, String rawPassword, int role) {
        User user = new User();
        user.setNickname(nickname);
        user.setPassword(BCrypt.hashpw(rawPassword, BCrypt.gensalt(BCRYPT_COST)));
        user.setRole(role);
        user.setStatus(1);
        user.setDeleted(0);
        userMapper.insert(user);
        return user;
    }

    public void updateLastLogin(Long userId) {
        User update = new User();
        update.setId(userId);
        update.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(update);
    }

    public void updateNickname(Long userId, String nickname) {
        User update = new User();
        update.setId(userId);
        update.setNickname(nickname);
        userMapper.updateById(update);
    }

    public void updatePassword(Long userId, String rawPassword) {
        User update = new User();
        update.setId(userId);
        update.setPassword(BCrypt.hashpw(rawPassword, BCrypt.gensalt(BCRYPT_COST)));
        userMapper.updateById(update);
    }

    public long countAdmins() {
        return userMapper.countAdmins();
    }

    public UserResp toResp(User user) {
        if (user == null) {
            return null;
        }
        return new UserResp(user.getId() == null ? null : String.valueOf(user.getId()),
                user.getNickname(), user.getRole());
    }
}
