package com.codeinterview.question.dto;

import lombok.Data;

@Data
public class QuestionQueryReq {

    private Long categoryId;
    private Long tagId;
    private Integer difficulty;
    private String keyword;
    /** v3.15：是否同时搜索题干/答案正文；默认 false = 只搜标题与标签（精准） */
    private Boolean searchBody;
    private String sort;
    private Integer page;
    private Integer pageSize;
}
