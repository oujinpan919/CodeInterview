package com.codeinterview.question.vo;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

/**
 * 列表/首页/收藏通用题目摘要（04 A1）。status 仅收藏/错题列表有值，公开列表为 null。
 */
@Data
public class QuestionBriefResp {

    private Long id;
    private String title;
    private Integer difficulty;
    private Long categoryId;
    private String categoryName;
    private List<TagBriefResp> tags;
    private Integer viewCount;
    private Integer practiceCount;
    private Integer status;
    /** v3.10：当前用户是否收藏（仅题库列表按登录态填充，其余场景为 null） */
    private Boolean favorite;
    /**
     * v4.1：当前用户是否打开过这道题的详情页（仅题库列表按登录态填充，其余场景为 null）。
     * 数据源是 question_view，与全站累计的 viewCount 无关；未登录恒为 null，前端不上色。
     */
    private Boolean visited;
    /** v3.15：关键词相关性得分（仅带 keyword 的列表查询填充，用于排序调试） */
    private Integer relevance;
    /** v3.15：命中位置，逗号分隔 title/tag/body（仅带 keyword 时填充），前端标「命中：标题/答案」 */
    private String matchFields;
    private LocalDateTime publishedAt;
}
