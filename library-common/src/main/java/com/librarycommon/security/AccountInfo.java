package com.librarycommon.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 账号信息（跨层传输用）
 *
 * <p>由 AccountService.loadByUsername 返回，避免 Spring 链路上直接传递 ORM 实体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountInfo implements Serializable {

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
     * 登录密码（BCrypt 哈希）
     */
    private String password;

    /**
     * 账号状态：0=禁用 / 1=正常
     */
    private Integer status;

    /**
     * 角色：0管理员 / 1读者
     */
    private Integer role;
}