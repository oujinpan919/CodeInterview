package com.codeinterview.user.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 用户（昵称即登录名，03 §3.1）。
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 昵称 = 登录名 + 展示名，2-20 字，全站唯一（大小写不敏感） */
    private String nickname;

    /** BCrypt 哈希（cost=12） */
    private String password;

    /** 0 普通用户 / 1 管理员 */
    private Integer role;

    /** 1 正常 / 0 禁用 */
    private Integer status;

    private LocalDateTime lastLoginAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
