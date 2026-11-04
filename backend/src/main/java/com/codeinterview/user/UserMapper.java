package com.codeinterview.user;

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
     */
    @Select("SELECT id, nickname, password, role, status, deleted FROM `user` WHERE nickname = #{nickname} LIMIT 1")
    User selectByNicknameIncludingDeleted(@Param("nickname") String nickname);

    @Select("SELECT id, nickname, password, role, status, deleted FROM `user` WHERE id = #{id} LIMIT 1")
    User selectByIdIncludingDeleted(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM `user` WHERE role = 1 AND deleted = 0")
    long countAdmins();
}
