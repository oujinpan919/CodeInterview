package com.codeinterview.path.vo;

import lombok.Data;

/**
 * 刷题路线列表项（04 C8 #51）。
 * completedCount 仅登录返回；游客为 null（前端显示「登录后记录进度」）。
 */
@Data
public class PathResp {

    private Long id;

    private String name;

    private String description;

    /** 有效题目数（只算发布态且分类启用） */
    private Long totalCount;

    /** 已自评过的去重题数；guest 为 null */
    private Long completedCount;
}
