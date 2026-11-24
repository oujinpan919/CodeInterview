package com.codeinterview.ai.vo;

import lombok.Data;

/**
 * Prompt 模板出参。
 */
@Data
public class PromptTemplateResp {

    private Long id;
    private String name;
    private String description;
    private Boolean isBuiltin;
    private Boolean enabled;
    private Integer sort;
    /** 仅管理员列表返回；普通用户拿不到正文，只用于选择。 */
    private String content;
}
