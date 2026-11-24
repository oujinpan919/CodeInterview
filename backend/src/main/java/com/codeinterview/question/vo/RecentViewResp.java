package com.codeinterview.question.vo;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * v4.2：最近浏览过的题目（题目练习页左侧「题目列表」的数据源）。
 *
 * <p>与 {@link QuestionBriefResp} 的区别：只服务于侧边题目切换，字段刻意精简
 * （不带标签/热度），并且一定带 lastViewedAt —— 前端要用它显示「上次浏览」。
 */
@Data
public class RecentViewResp {

    private Long id;
    private String title;
    private Integer difficulty;
    private Long categoryId;
    private String categoryName;
    /** 最近一次打开该题详情的时间（question_view.last_viewed_at） */
    private LocalDateTime lastViewedAt;
}
