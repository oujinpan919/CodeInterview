package com.codeinterview.auth.dto;

import jakarta.validation.constraints.Size;

import lombok.Data;

/**
 * v4.12 设置头像。avatar 传 OSS 返回的 URL；传空字符串表示清除（回退首字母色块）。
 *
 * <p>不加 @NotBlank：清除头像就是合法的空值操作。
 * 长度上限 512 与 user.avatar 列宽一致，避免写库时被截断。
 */
@Data
public class AvatarUpdateReq {

    @Size(max = 512, message = "头像地址过长")
    private String avatar;
}
