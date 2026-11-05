package com.codeinterview.stats.vo;

import java.time.LocalDate;

import lombok.Data;

/**
 * 趋势聚合行（内部用）：SQL 返回 DATE，Java 侧补连续日期。
 */
@Data
public class TrendRow {

    private LocalDate day;
    private long count;
}
