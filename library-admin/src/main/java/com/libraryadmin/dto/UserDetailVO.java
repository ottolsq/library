package com.libraryadmin.dto;

import com.librarycommon.enums.UserStatus;

import java.time.LocalDateTime;

/**
 * 系统用户详情 VO
 */
public record UserDetailVO(
        Long userId,
        String username,
        Integer role,
        UserStatus status,
        LocalDateTime createTime
) {
}