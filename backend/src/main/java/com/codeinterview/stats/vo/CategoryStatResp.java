package com.codeinterview.stats.vo;

import lombok.Data;

/**
 * 分类分布（04 A3.5 #21）：count 按记录条数聚合，合计等于 overview 的 totalRecordCount。
 */
@Data
public class CategoryStatResp {

    private Long categoryId;
    private String name;
    private long count;
    private Double masteredRate;
}
