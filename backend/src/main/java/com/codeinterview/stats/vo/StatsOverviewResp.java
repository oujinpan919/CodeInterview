package com.codeinterview.stats.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

/**
 * 指标卡（04 A3.5 #19）。
 * 两个总量口径不同：totalQuestionCount = COUNT(DISTINCT question_id)（累计自评题数），
 * totalRecordCount = COUNT(*)（累计自评条数），不可混用。
 */
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class StatsOverviewResp {

    private long todayCount;
    private long weekCount;
    /** 0–1 小数；无数据时为 null，前端显示「—」 */
    private Double masteredRate;
    private int streak;
    private long totalQuestionCount;
    private long totalRecordCount;
}
