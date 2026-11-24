package com.codeinterview.ai.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 会话消息（D6 / migration-v5 §6）。role 只可能是 user / assistant。
 */
@Data
@TableName("ai_message")
public class AiMessage {

    /** 与 ai_chat_log.status 同口径：1 正常 / 2 失败 / 3 客户端中断。 */
    public static final int STATUS_OK = 1;
    public static final int STATUS_FAILED = 2;
    public static final int STATUS_ABORTED = 3;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long conversationId;
    private String role;
    private String content;
    private Integer status;
    private String statusNote;
    private Integer promptTokens;
    private Integer completionTokens;
    private LocalDateTime createdAt;
}
