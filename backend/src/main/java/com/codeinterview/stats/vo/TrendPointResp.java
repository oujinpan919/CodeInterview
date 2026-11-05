package com.codeinterview.stats.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 趋势点（04 A3.5 #20）：date 为 yyyy-MM-dd，连续日期缺失补 0。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendPointResp {

    private String date;
    private long count;
}
