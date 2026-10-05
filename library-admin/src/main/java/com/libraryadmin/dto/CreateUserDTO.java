package com.libraryadmin.dto;

import com.librarycommon.enums.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 新增系统用户请求参数
 *
 * @param username 用户名
 * @param password 明文密码（接口层做 BCrypt 哈希后入库）
 * @param role     角色：0 管理员 / 1 读者
 * @param status   账号状态
 */
public record CreateUserDTO(
        @NotBlank(message = "账号不能为空")
        @Size(min = 3, max = 32, message = "账号长度应为 3-32 位")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度应为 6-64 位")
        String password,

        @NotNull(message = "角色不能为空")
        Integer role,

        @NotNull(message = "账号状态不能为空")
        UserStatus status
) {
}