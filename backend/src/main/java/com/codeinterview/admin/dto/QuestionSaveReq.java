package com.codeinterview.admin.dto;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端新建 / 更新题目入参（04 A3.9 #29/#30）。
 */
@Data
public class QuestionSaveReq {

    @NotBlank(message = "标题不能为空")
    @Size(max = 120, message = "标题不能超过 120 字")
    private String title;

    @NotBlank(message = "题干不能为空")
    @Size(max = 20000, message = "题干不能超过 20000 字符")
    private String contentMd;

    @NotBlank(message = "参考答案不能为空")
    @Size(max = 20000, message = "参考答案不能超过 20000 字符")
    private String answerMd;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @NotNull(message = "难度不能为空")
    @Min(value = 1, message = "难度只能为 1/2/3")
    @Max(value = 3, message = "难度只能为 1/2/3")
    private Integer difficulty;

    /** ≤10 个；提交即覆盖式更新（04 A3.9 #29） */
    private List<Long> tagIds;

    @Size(max = 50, message = "来源不能超过 50 字")
    private String source;
}
