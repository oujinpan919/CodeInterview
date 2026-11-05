package com.codeinterview.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端分类新建 / 更新入参（04 A3.9 #36/#37）。
 */
@Data
public class CategorySaveReq {

    @NotBlank(message = "分类名不能为空")
    @Size(max = 30, message = "分类名不能超过 30 字")
    private String name;

    @Size(max = 200, message = "说明不能超过 200 字")
    private String description;

    private Integer sort;

    @Min(value = 0, message = "状态只能为 0/1")
    @Max(value = 1, message = "状态只能为 0/1")
    private Integer status;
}
