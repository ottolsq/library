package com.libraryadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 重置密码请求参数
 */
public record ResetPasswordDTO(
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度应为 6-64 位")
        String newPassword
) {
}