package com.libraryweb.dto;

import java.util.Set;

/**
 * 当前登录用户信息 VO
 *
 * <p>从 SecurityContext 中的 {@link com.librarycommon.security.LoginUser} 投影而来，
 * 仅返回前端展示所需的字段，不暴露密码等敏感信息。
 */
public record CurrentUserVO(
        Long userId,
        String username,
        Boolean enabled,
        Set<String> roles,
        Set<String> permissions,
        boolean superAdmin
) {
}
