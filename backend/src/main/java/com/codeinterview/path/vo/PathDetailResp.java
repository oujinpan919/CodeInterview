package com.codeinterview.path.vo;

import java.util.List;

import lombok.Data;

/**
 * 路线详情（04 C8 #52）：路线元信息 + 有序题目列表（含完成标记）。
 */
@Data
public class PathDetailResp {

    private Long id;

    private String name;

    private String description;

    private Long totalCount;

    /** 已自评去重题数；guest 为 null */
    private Long completedCount;

    private List<PathItemResp> items;
}
