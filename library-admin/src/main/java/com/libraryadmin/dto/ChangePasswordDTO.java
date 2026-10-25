package com.libraryadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 本人修改密码请求参数
 *
 * @param oldPassword 原密码，用于校验身份
 * @param newPassword 新密码
 */
public record ChangePasswordDTO(
        @NotBlank(message = "原密码不能为空")
        String oldPassword,

        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度应为 6-64 位")
        String newPassword
) {
}
