package com.libraryadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 读者自助注册请求参数。
 *
 * <p>注册只创建读者角色，不接收 role 字段，避免越权注册管理员。
 *
 * @param username        登录账号
 * @param password        登录密码
 * @param confirmPassword 确认密码
 * @param readerName      读者姓名
 * @param studentId       学号
 * @param phone           手机号（可选）
 */
public record RegisterDTO(
        @NotBlank(message = "账号不能为空")
        @Size(min = 3, max = 32, message = "账号长度应为 3-32 位")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度应为 6-64 位")
        String password,

        @NotBlank(message = "确认密码不能为空")
        String confirmPassword,

        @NotBlank(message = "姓名不能为空")
        @Size(max = 50, message = "姓名不能超过 50 字")
        String readerName,

        @NotBlank(message = "学号不能为空")
        @Size(max = 30, message = "学号不能超过 30 位")
        String studentId,

        @Size(max = 20, message = "手机号不能超过 20 位")
        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone
) {
}
