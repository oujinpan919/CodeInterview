package com.codeinterview.note.dto;

import lombok.Data;

/**
 * 保存笔记入参（04 C5 #48）。contentMd 允许为空串 —— 空串语义是「删除这条笔记」。
 */
@Data
public class NoteSaveReq {

    private String contentMd;
}
