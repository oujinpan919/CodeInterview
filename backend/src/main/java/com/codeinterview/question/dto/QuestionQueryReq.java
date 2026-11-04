package com.codeinterview.question.dto;

import lombok.Data;

@Data
public class QuestionQueryReq {

    private Long categoryId;
    private Long tagId;
    private Integer difficulty;
    private String keyword;
    private String sort;
    private Integer page;
    private Integer pageSize;
}
