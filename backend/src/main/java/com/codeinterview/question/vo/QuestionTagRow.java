package com.codeinterview.question.vo;

import lombok.Data;

/**
 * 题目标签批量查询的中间行（question_tag + tag），由 Service 按 questionId 分组组装。
 */
@Data
public class QuestionTagRow {

    private Long questionId;
    private Long tagId;
    private String tagName;
}
