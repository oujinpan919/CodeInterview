package com.codeinterview.question.vo;

import lombok.Data;

@Data
public class TagBriefResp {

    private Long id;
    private String name;

    public TagBriefResp() {
    }

    public TagBriefResp(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
