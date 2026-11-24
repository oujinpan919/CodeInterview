package com.codeinterview.ai.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 会话（D6 / migration-v5 §6）。替换 05 §1「服务端不持久化消息正文」红线。
 */
@Data
@TableName("ai_conversation")
public class AiConversation {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private Long questionId;
    private Long modelConfigId;
    private Integer messageCount;
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
