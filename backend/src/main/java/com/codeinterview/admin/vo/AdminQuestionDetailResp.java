package com.codeinterview.admin.vo;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

/**
 * 管理端题目编辑详情（04 A3.9 #28）。
 */
@Data
public class AdminQuestionDetailResp {

    private Long id;
    private String title;
    private String contentMd;
    private String answerMd;
    private Long categoryId;
    private Integer difficulty;
    private List<Long> tagIds;
    private String source;
    private Integer status;
    private LocalDateTime publishedAt;
}
