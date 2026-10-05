package com.libraryweb.dto;

import java.util.Set;

/**
 * 登录响应
 *
 * @param token       JWT 令牌字符串（前端保存到请求头）
 * @param expiresIn   令牌有效期秒数
 * @param username    登录账号
 * @param nickname    用户姓名
 * @param roles       角色编码列表
 * @param permissions 权限标识列表
 */
public record LoginVO(
        String token,
        Long expiresIn,
        String username,
        String nickname,
        Set<String> roles,
        Set<String> permissions
) {
}