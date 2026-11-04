package com.codeinterview.taxonomy.vo;

import lombok.Data;

@Data
public class CategoryResp {

    private Long id;
    private String name;
    private String description;
    private Integer sort;
    private Integer questionCount;
}
