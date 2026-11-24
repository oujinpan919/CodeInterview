package com.codeinterview.ai.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 凭证密文（D3 / migration-v5 §3）。明文永不入库、永不回显、永不进日志。
 */
@Data
@TableName("ai_credential")
public class AiCredential {

    public static final int OWNER_USER = 1;
    public static final int OWNER_SITE = 2;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer ownerType;
    private Long ownerId;
    private String cipherAlgo;
    private Integer keyVersion;
    private byte[] iv;
    private byte[] tag;
    private byte[] payload;
    private String keyHint;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
