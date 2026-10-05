package com.libraryadmin.dto;

import com.librarycommon.enums.UserStatus;
import jakarta.validation.constraints.Size;

/**
 * 修改系统用户资料请求参数
 *
 * <p>用户名、角色与状态允许修改，密码不在此处改，由专门的「重置密码」接口处理。
 */
public record UpdateUserDTO(
        @Size(min = 3, max = 32, message = "账号长度应为 3-32 位")
        String username,

        Integer role,
        UserStatus status
) {
}