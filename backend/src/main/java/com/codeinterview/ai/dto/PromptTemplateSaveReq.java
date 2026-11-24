package com.codeinterview.ai.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Prompt 模板保存请求（D11）。仅管理员可用。
 */
@Data
public class PromptTemplateSaveReq {

    @NotBlank
    @Size(max = 32)
    private String name;

    @NotBlank
    @Size(max = 8000)
    private String content;

    @Size(max = 255)
    private String description;

    private Boolean enabled;
    private Integer sort;
}
