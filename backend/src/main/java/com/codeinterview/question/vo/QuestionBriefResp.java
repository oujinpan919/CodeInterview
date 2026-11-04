package com.codeinterview.question.vo;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

/**
 * 列表/首页/收藏通用题目摘要（04 A1）。status 仅收藏/错题列表有值，公开列表为 null。
 */
@Data
public class QuestionBriefResp {

    private Long id;
    private String title;
    private Integer difficulty;
    private Long categoryId;
    private String categoryName;
    private List<TagBriefResp> tags;
    private Integer viewCount;
    private Integer practiceCount;
    private Integer status;
    private LocalDateTime publishedAt;
}
