package com.codeinterview.question.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 题目写侧实体（03 §3.4）。读侧仍用 QuestionMapper 的手写 SQL（带 JOIN 与状态条件），
 * 这里只服务管理端的增删改，@TableLogic 让 deleteById 变成逻辑删除（deleted=1）。
 */
@Data
@TableName("question")
public class Question {

    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_PUBLISHED = 1;
    public static final int STATUS_OFFLINE = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;

    private String title;

    private String contentMd;

    private String answerMd;

    /** 1 简单 / 2 中等 / 3 困难 */
    private Integer difficulty;

    /** 0 草稿 / 1 发布 / 2 下架 */
    private Integer status;

    private String source;

    private Integer viewCount;

    private Integer practiceCount;

    private LocalDateTime publishedAt;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
