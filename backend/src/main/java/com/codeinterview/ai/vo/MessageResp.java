package com.codeinterview.ai.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class MessageResp {

    private Long id;
    private String role;
    private String content;
    private Integer status;
    private String statusNote;
    private LocalDateTime createdAt;
}
