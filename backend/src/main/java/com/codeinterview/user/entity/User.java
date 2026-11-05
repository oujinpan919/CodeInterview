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

    /* 02 §7 禁止魔法值：角色 / 状态 / 逻辑删除统一用常量（与 03 DDL 注释一致） */
    public static final int ROLE_NORMAL = 0;
    public static final int ROLE_ADMIN = 1;
    public static final int STATUS_DISABLED = 0;
    public static final int STATUS_NORMAL = 1;
    public static final int NOT_DELETED = 0;
    public static final int DELETED = 1;

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
