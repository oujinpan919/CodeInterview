package com.codeinterview.stats.vo;

import lombok.Data;

@Data
public class DifficultyStatResp {

    private Integer difficulty;
    private Long count;
    private Double masteredRate;
}
