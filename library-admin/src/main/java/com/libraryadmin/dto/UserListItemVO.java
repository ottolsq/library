package com.libraryadmin.dto;

import com.librarycommon.enums.UserStatus;

/**
 * 用户列表返回项
 */
public record UserListItemVO(
        Long userId,
        String username,
        Integer role,
        UserStatus status
) {
}