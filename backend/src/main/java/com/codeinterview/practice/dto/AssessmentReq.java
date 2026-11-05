package com.codeinterview.practice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 三档自评入参（04 A3.4 #14）：selfAssess 必填 1/2/3，source 可选（1 详情 / 3 错题本复习）。
 */
@Data
public class AssessmentReq {

    @NotNull(message = "selfAssess 不能为空")
    @Min(value = 1, message = "selfAssess 只能为 1/2/3")
    @Max(value = 3, message = "selfAssess 只能为 1/2/3")
    private Integer selfAssess;

    private Integer source;
}
