package com.codeinterview.note.vo;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 笔记导出聚合行（04 C5 #49）：按分类排序后拼 Markdown。
 * 题干取标题即可，导出物是「我的笔记」而不是题库副本。
 */
@Data
public class QuestionNoteRow {

    private Long questionId;

    private String title;

    private Long categoryId;

    private String categoryName;

    private Integer categorySort;

    private String contentMd;

    private LocalDateTime updatedAt;
}
