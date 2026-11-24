package com.codeinterview.ai.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 配置变更审计（D14 / migration-v5 §7）。{@code detail} 不得含 Key 明文。
 */
@Data
@TableName("ai_config_audit")
public class AiConfigAudit {

    public static final String TARGET_USER = "user_config";
    public static final String TARGET_SITE = "site_config";
    public static final String TARGET_CREDENTIAL = "credential";

    public static final String ACTION_CREATE = "create";
    public static final String ACTION_UPDATE = "update";
    public static final String ACTION_DELETE = "delete";
    public static final String ACTION_TEST = "test";
    public static final String ACTION_SET_DEFAULT = "set_default";
    public static final String ACTION_EXPORT_KEY = "export_key";

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String targetType;
    private Long targetId;
    private String action;
    private String detail;
    private LocalDateTime createdAt;
}
