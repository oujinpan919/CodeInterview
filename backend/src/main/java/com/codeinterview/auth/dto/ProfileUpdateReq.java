package com.codeinterview.auth.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

/** 04 A3.1 #5：改昵称需当前密码二次确认。 */
@Data
public class ProfileUpdateReq {

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    @NotBlank(message = "当前密码不能为空")
    private String password;
}
