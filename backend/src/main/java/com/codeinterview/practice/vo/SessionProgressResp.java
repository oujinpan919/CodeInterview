package com.codeinterview.practice.vo;

import lombok.Data;

@Data
public class SessionProgressResp {

    private Long sessionId;

    private Integer status;

    private Integer position;

    private Integer totalCount;

    private Integer completedCount;

    private Integer skippedCount;

    private Long currentQuestionId;

    private Integer skippedNotice;
}
