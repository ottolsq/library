package com.libraryweb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 登录请求参数
 *
 * @param username 登录账号
 * @param password 登录密码
 */
public record LoginDTO(
        @NotBlank(message = "账号不能为空")
        @Size(min = 3, max = 32, message = "账号长度应为 3-32 位")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度应为 6-64 位")
        String password
) {
}