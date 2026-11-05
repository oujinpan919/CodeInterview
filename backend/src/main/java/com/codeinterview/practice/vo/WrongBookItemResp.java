package com.codeinterview.practice.vo;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 错题本列表行（04 A3.4 #16）。
 * status 与 QuestionBrief.status 同名同义（2=已下架，前端据此置灰「去复习」）。
 */
@Data
public class WrongBookItemResp {

    private Long questionId;
    private String title;
    private Integer difficulty;
    private String categoryName;
    private Integer lastAssess;
    private Integer wrongCount;
    private LocalDateTime lastWrongAt;
    private Integer resolved;
    private Integer status;
}
