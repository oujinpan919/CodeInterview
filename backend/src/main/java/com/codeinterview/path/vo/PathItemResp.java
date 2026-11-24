package com.codeinterview.path.vo;

import java.util.List;

import com.codeinterview.question.vo.TagBriefResp;

import lombok.Data;

/**
 * 路线内一道题（04 C8 #52 / v3.9）。done = 当前用户对该题是否有过任意自评；
 * favorite = 是否已收藏（游客恒 false）；tags 供表格「标签」列使用（批量回填，不 N+1）。
 */
@Data
public class PathItemResp {

    private Long questionId;

    private Integer sort;

    private String title;

    private Integer difficulty;

    private String categoryName;

    private Boolean done;

    /** v3.9：当前用户是否收藏了这道题（游客 / 未收藏为 false）；表格「标记」列用 */
    private Boolean favorite;

    /** v3.9：题目标签（最多展示 3 个，其余折叠成 +N） */
    private List<TagBriefResp> tags;
}
