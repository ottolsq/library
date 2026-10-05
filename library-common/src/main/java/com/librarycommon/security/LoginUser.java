package com.librarycommon.security;

import com.librarycommon.constant.SecurityConstants;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 登录用户详情
 *
 * <p>实现 Spring Security 的 {@link UserDetails}，作为 SecurityContext 与 Redis 缓存的载体。
 * <p>提供：
 * <ul>
 *   <li>用户 ID / 登录账号 / 密码 / 账号状态</li>
 *   <li>角色编码集合（ROLE_xxx）</li>
 *   <li>权限标识集合（如 user:add、book:list）</li>
 * </ul>
 *
 * <p><b>注意</b>：
 * <ul>
 *   <li>权限字段始终返回非空集合，避免后续比对时层层判空</li>
 *   <li>作为缓存对象，必须提供无参构造方法，反序列化时会先创建空对象再逐字段赋值</li>
 * </ul>
 */
@Data
@NoArgsConstructor
public class LoginUser implements UserDetails, Serializable {

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
     * 账号状态：true 正常 / false 禁用
     */
    private Boolean enabled;

    /**
     * 角色编码集合（ROLE_xxx）
     */
    private Set<String> roles;

    /**
     * 权限标识集合（如 user:add）
     */
    private Set<String> permissions;

    /**
     * 构造：最小必要字段
     */
    public LoginUser(Long userId, String username, String password,
                     Boolean enabled, Set<String> roles, Set<String> permissions) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        // 防御性初始化：保证永远非空，避免后续判空
        this.roles = roles == null ? new HashSet<>() : roles;
        this.permissions = permissions == null ? new HashSet<>() : permissions;
    }

    /**
     * 是否超级管理员：角色集合中包含 ROLE_ADMIN
     */
    public boolean isSuperAdmin() {
        return roles != null && roles.contains(SecurityConstants.ROLE_ADMIN);
    }

    /* ================== UserDetails ================== */

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled != null && enabled;
    }
}