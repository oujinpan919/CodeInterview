package com.codeinterview.practice.vo;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 错题本列表行（04 A3.4 #16；v3.6 增 due_at / interval_days / review_streak / stage）。
 *
 * <p>status 与 QuestionBrief.status 同名同义（2=已下架，前端据此置灰「去复习」）。
 * stage 是 v3.6 的三态：learning 在学 / reviewing 已掌握待回顾 / mastered 真掌握（已退出复习队列）。
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
    /** 下次复习到期时间；null = 已退出复习队列（真掌握） */
    private LocalDateTime dueAt;
    /** 当前复习间隔（天）；0 = 未排程 */
    private Integer intervalDays;
    /** 连续掌握次数，达到 4 判真掌握 */
    private Integer reviewStreak;
    /** learning / reviewing / mastered */
    private String stage;
    /** 是否写过笔记（v3.6 C6：错题本行显示「笔记」标记，见 04 C3） */
    private Boolean hasNote;
}
