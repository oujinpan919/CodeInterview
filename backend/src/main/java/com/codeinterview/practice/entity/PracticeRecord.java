package com.codeinterview.practice.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 刷题记录（03 §3.7）：只增不改不删，自评一次一行。
 */
@Data
@TableName("practice_record")
public class PracticeRecord {

    public static final int ASSESS_MASTERED = 1;
    public static final int ASSESS_FUZZY = 2;
    public static final int ASSESS_UNKNOWN = 3;

    public static final int SOURCE_DETAIL = 1;
    public static final int SOURCE_RANDOM = 2;
    public static final int SOURCE_WRONG_BOOK = 3;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long questionId;

    /** 1 掌握 / 2 模糊 / 3 不会 */
    private Integer selfAssess;

    /** 1 详情页 / 2 随机刷题（V1 不用） / 3 错题本复习 */
    private Integer source;

    private LocalDateTime createdAt;
}
