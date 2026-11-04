package com.codeinterview.question.vo;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

/**
 * 题目详情主体（04 A3.3 #9 的 question 字段）。
 */
@Data
public class QuestionDetail {

    private Long id;
    private String title;
    private String contentMd;
    private String answerMd;
    private Integer difficulty;
    private Long categoryId;
    private String categoryName;
    private List<TagBriefResp> tags;
    private Integer viewCount;
    private Integer practiceCount;
    private LocalDateTime publishedAt;
}
