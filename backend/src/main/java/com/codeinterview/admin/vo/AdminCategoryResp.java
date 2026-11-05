package com.codeinterview.admin.vo;

import lombok.Data;

/**
 * 管理端分类行（04 A3.9 #35）：含停用分类；questionCount 按未删除题计，publishedCount 按发布态计。
 */
@Data
public class AdminCategoryResp {

    private Long id;
    private String name;
    private String description;
    private Integer sort;
    private Integer status;
    private long questionCount;
    private long publishedCount;
}
