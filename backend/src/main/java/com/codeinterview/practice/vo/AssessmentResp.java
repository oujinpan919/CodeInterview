package com.codeinterview.practice.vo;

import lombok.Data;

/**
 * 三档自评响应（04 A3.4 #14）。
 */
@Data
public class AssessmentResp {

    private Long practiceId;

    /** null 表示从未进过错题本，前端据此决定是否提示「已加入错题本」 */
    private WrongBookState wrongBook;

    /** 今日已刷数（当日自然日自评条数） */
    private long todayCount;
}
