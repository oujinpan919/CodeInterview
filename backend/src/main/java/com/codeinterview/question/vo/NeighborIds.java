package com.codeinterview.question.vo;

import lombok.Data;

/**
 * 一次窗口查询同时取回前后相邻题（v3.6 修1）。
 * 与 {@link NeighborResp} 分开是因为这里直接承载 MyBatis 结果集（prev_id / next_id），
 * 对外的响应体仍然用 NeighborResp，避免 SQL 细节泄漏到接口契约。
 */
@Data
public class NeighborIds {

    private Long prevId;

    private Long nextId;
}
