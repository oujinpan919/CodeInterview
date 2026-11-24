package com.codeinterview.practice.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("practice_session")
public class PracticeSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer status;

    // 可空标记列 + 唯一索引：NULL 不参与唯一约束，同一用户至多 1 个进行中；结束置 NULL 释放名额且保留历史
    private Integer activeFlag;

    private String questionIds;

    private Integer totalCount;

    private Integer currentIndex;

    private Integer completedCount;

    private Integer skippedCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
