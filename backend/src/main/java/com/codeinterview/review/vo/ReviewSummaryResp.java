package com.codeinterview.review.vo;

import lombok.Data;

/**
 * 今日计划（04 A3.2 #7 todayPlan / A3.6 #22）。
 *
 * <p>只在登录时返回；游客为 null。estimatedMinutes 是经验估算，不是承诺：
 * 待复习题按 0.8 分钟/题、新题按 1.2 分钟/题（含读题与自评动作），向上取整。
 */
@Data
public class ReviewSummaryResp {

    /** 今日到期待复习题数（含逾期） */
    private long dueCount;

    /** 其中已逾期题数（due_at 早于当前时刻） */
    private long overdueCount;

    /** 今日新题池建议数量（实际取数受当日剩余新题数限制） */
    private long newCount;

    /** 估算分钟数 */
    private long estimatedMinutes;
}
