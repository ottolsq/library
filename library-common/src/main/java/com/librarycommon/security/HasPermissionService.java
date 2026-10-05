package com.librarycommon.security;

import com.librarycommon.constant.SecurityConstants;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 权限判断服务
 *
 * <p>路径级授权（如 {@code authorizeHttpRequests}）与方法注解（如 {@code @PreAuthorize}）
 * 必须共用这一处逻辑，否则规则演进时两处会不一致。
 *
 * <p>判断逻辑分三步：
 * <ol>
 *   <li>从 {@link SecurityContextHolder} 取出当前登录用户，取不到或权限集合为空则直接返回不通过</li>
 *   <li>权限集合中含有通配符（{@link SecurityConstants#ALL_PERMISSION}）则说明是超级管理员，直接返回通过</li>
 *   <li>否则判断权限/角色集合是否包含目标标识</li>
 * </ol>
 *
 * <p><b>命名约定</b>：Bean 名固定为 {@code ss}（Spring Security 权限校验的标准短名）。
 */
@Service("ss")
public class HasPermissionService {

    /**
     * 通配符权限：拥有此权限即视为超级管理员
     */
    private static final String SUPER_ADMIN_PERMISSION = SecurityConstants.ALL_PERMISSION;

    /**
     * 判断当前登录用户是否拥有指定权限
     *
     * @param permission 权限标识（如 {@code user:add}）
     * @return true 通过 / false 不通过
     */
    public boolean hasPermission(String permission) {
        Set<String> permissions = currentPermissions();
        if (permissions.isEmpty() || permission == null) {
            return false;
        }
        // 超级管理员通配符
        if (permissions.contains(SUPER_ADMIN_PERMISSION)) {
            return true;
        }
        return permissions.contains(permission);
    }

    /**
     * 判断当前登录用户是否拥有指定权限集合中的任意一个
     *
     * @param permissions 权限标识集合
     * @return true 通过 / false 不通过
     */
    public boolean hasAnyPermission(Set<String> permissions) {
        Set<String> current = currentPermissions();
        if (current.isEmpty() || permissions == null || permissions.isEmpty()) {
            return false;
        }
        if (current.contains(SUPER_ADMIN_PERMISSION)) {
            return true;
        }
        for (String p : permissions) {
            if (current.contains(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断当前登录用户是否拥有指定角色
     *
     * @param role 角色编码（如 {@code ROLE_ADMIN}）
     * @return true 通过 / false 不通过
     */
    public boolean hasRole(String role) {
        Set<String> roles = currentRoles();
        if (roles.isEmpty() || role == null) {
            return false;
        }
        // 超级管理员角色
        if (roles.contains(SecurityConstants.ROLE_ADMIN)) {
            return true;
        }
        return roles.contains(role);
    }

    /**
     * 判断当前登录用户是否为超级管理员
     *
     * @return true / false
     */
    public boolean isSuperAdmin() {
        Set<String> roles = currentRoles();
        if (roles.isEmpty()) {
            return false;
        }
        return roles.contains(SecurityConstants.ROLE_ADMIN);
    }

    /* ================== 私有工具方法 ================== */

    /**
     * 从安全上下文取出当前登录用户的权限集合
     *
     * @return 权限集合；取不到时返回空集合（便于直接判断 isEmpty）
     */
    private Set<String> currentPermissions() {
        LoginUser loginUser = currentLoginUser();
        if (loginUser == null) {
            return Set.of();
        }
        Set<String> permissions = loginUser.getPermissions();
        return permissions == null ? Set.of() : permissions;
    }

    /**
     * 从安全上下文取出当前登录用户的角色集合
     */
    private Set<String> currentRoles() {
        LoginUser loginUser = currentLoginUser();
        if (loginUser == null) {
            return Set.of();
        }
        Set<String> roles = loginUser.getRoles();
        return roles == null ? Set.of() : roles;
    }

    /**
     * 从安全上下文取出当前登录用户
     */
    private LoginUser currentLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof LoginUser loginUser)) {
            return null;
        }
        return loginUser;
    }
}