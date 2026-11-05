package com.codeinterview.practice.vo;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 刷题记录列表行（04 A3.4 #15）。
 */
@Data
public class PracticeRecordResp {

    private Long id;
    private Long questionId;
    private String title;
    private Integer difficulty;
    private String categoryName;
    private Integer selfAssess;
    private Integer source;
    private LocalDateTime createdAt;
}
