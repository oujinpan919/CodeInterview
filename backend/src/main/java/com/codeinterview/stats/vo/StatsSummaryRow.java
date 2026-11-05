package com.codeinterview.stats.vo;

import lombok.Data;

/**
 * overview 聚合行（内部用）：masteredCount / totalCount 用于算掌握率。
 */
@Data
public class StatsSummaryRow {

    private long todayCount;
    private long weekCount;
    private long totalCount;
    private long masteredCount;
}
