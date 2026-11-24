package com.codeinterview.ai.vo;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class ConversationDetailResp {

    private Long id;
    private String title;
    private Long questionId;
    private Long modelConfigId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MessageResp> messages;
}
