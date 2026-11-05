package com.codeinterview.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端标签新建 / 更新入参（04 A3.9 #39/#40）。
 */
@Data
public class TagSaveReq {

    @NotBlank(message = "标签名不能为空")
    @Size(max = 20, message = "标签名不能超过 20 字")
    private String name;
}
