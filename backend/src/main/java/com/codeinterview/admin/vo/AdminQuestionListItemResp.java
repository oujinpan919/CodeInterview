package com.codeinterview.admin.vo;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 管理端题目列表行（04 A3.9 #27）：含全部状态与 source。
 */
@Data
public class AdminQuestionListItemResp {

    private Long id;
    private String title;
    private Long categoryId;
    private String categoryName;
    private Integer difficulty;
    private Integer status;
    private String source;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;
}
