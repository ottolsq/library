package com.librarycommon.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登录用户信息
 *
 * <p>存储在 JWT 与 Redis 中，用于认证与权限判断。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUserDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户主键ID
     */
    private Long userId;

    /**
     * 登录账号
     */
    private String username;

    /**
     * 角色：0管理员，1读者
     */
    private Integer role;

    /**
     * 判断是否为管理员
     */
    public boolean isAdmin() {
        return role != null && role == 0;
    }
}
