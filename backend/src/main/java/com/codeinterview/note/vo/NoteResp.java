package com.codeinterview.note.vo;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 题目笔记（04 C5 #48）。未写过时 contentMd 为 ""、updatedAt 为 null。
 */
@Data
public class NoteResp {

    private String contentMd;

    private LocalDateTime updatedAt;
}
