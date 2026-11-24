package com.codeinterview.practice.vo;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 自评接口返回的复习状态（04 A3.4 #14；v3.6 增排程字段）。
 *
 * <p>null = 既没有自评记录、也没有复习条目（v3.6 起仅当题目从未被自评时出现）；
 * 有值时 stage 为 learning / reviewing / mastered 三态之一。
 */
@Data
public class WrongBookState {

    private Integer resolved;

    private Integer wrongCount;

    /** 下次复习到期时间；null = 已退出复习队列（真掌握） */
    private LocalDateTime dueAt;

    private Integer intervalDays;

    private Integer reviewStreak;

    /** learning 在学 / reviewing 已掌握待回顾 / mastered 真掌握 */
    private String stage;
}
