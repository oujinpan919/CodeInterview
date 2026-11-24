package com.codeinterview.user;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codeinterview.user.entity.User;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 查重包含已逻辑删除的行：昵称永不复用（03 §4.4），命中 deleted=1 也算不可用。
     * 依赖 uk_nickname 在 utf8mb4_0900_ai_ci 下天然大小写不敏感，禁止用 LOWER() 包字段。
     * v4.10：补 created_at / last_login_at，个人中心要展示「加入时间 / 最近登录」；
     *        自定义 @Select 不会自动带全列，漏掉就会静默返回 null。
     * v4.12：补 avatar（同一坑，加列后忘记同步 @Select 会让头像永远为空）。
     */
    @Select("SELECT id, nickname, avatar, password, role, status, last_login_at, created_at, deleted"
            + " FROM `user` WHERE nickname = #{nickname} LIMIT 1")
    User selectByNicknameIncludingDeleted(@Param("nickname") String nickname);

    @Select("SELECT id, nickname, avatar, password, role, status, last_login_at, created_at, deleted"
            + " FROM `user` WHERE id = #{id} LIMIT 1")
    User selectByIdIncludingDeleted(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM `user` WHERE role = 1 AND deleted = 0")
    long countAdmins();

    @Select("SELECT id, nickname, avatar, password, role, status, last_login_at, created_at, deleted"
            + " FROM `user` WHERE role = 1 AND deleted = 0")
    List<User> selectAdmins();
}
