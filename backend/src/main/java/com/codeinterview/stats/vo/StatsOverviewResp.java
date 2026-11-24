package com.codeinterview.stats.vo;

import java.util.List;

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
    private List<DifficultyStatResp> difficultyDistribution;
    private Long completedSessionCount;
    /** 发布态题目总数（覆盖率分母） */
    private long publishedQuestionCount;
    /** 覆盖率 = 累计自评去重题数 ÷ 发布态题目总数；0–1 小数，分母为 0 时 null */
    private Double coverageRate;
}
