package com.codeinterview.ai.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ConversationResp {

    private Long id;
    private String title;
    private Long questionId;
    private Integer messageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
