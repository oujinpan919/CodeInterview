package com.codeinterview.practice.vo;

import lombok.Data;

/**
 * 错题本分视图计数（04 A3.4 #19，v3.6 新增）。
 * 一次请求返回三个 tab 的角标，避免前端串行打三次列表接口。
 */
@Data
public class WrongBookSummaryResp {

    /** 在学（resolved=0） */
    private long learning;

    /** 已掌握待回顾（resolved=1 且仍在复习队列） */
    private long reviewing;

    /** 真掌握（连续掌握 4 次，已退出复习队列） */
    private long mastered;

    /** 全部复习条目 */
    private long total;
}
