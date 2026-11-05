package com.codeinterview.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 批量导入入参（04 A3.9 #34）：请求体为 { "json": "<JSON 原文>" }。
 */
@Data
public class ImportReq {

    @NotBlank(message = "json 不能为空")
    private String json;
}
