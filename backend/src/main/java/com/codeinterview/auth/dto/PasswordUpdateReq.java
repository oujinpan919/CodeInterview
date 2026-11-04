package com.codeinterview.auth.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

/** 04 A3.1 #6：改密码需原密码。 */
@Data
public class PasswordUpdateReq {

    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
