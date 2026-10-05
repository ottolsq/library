package com.libraryweb.security;

import com.librarycommon.constant.SecurityConstants;
import com.librarycommon.enums.UserStatus;
import com.librarycommon.security.AccountInfo;
import com.librarycommon.security.AccountService;
import com.librarycommon.security.LoginUser;
import com.librarycommon.security.PermissionService;
import com.librarycommon.security.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * 用户详情服务
 *
 * <p>实现 Spring Security 的 {@link UserDetailsService}，对外只暴露按用户名加载用户详情的能力。
 *
 * <p>loadUserByUsername 装配流程（六步）：
 * <ol>
 *   <li>按登录账号查询账号（逻辑删除字段由 MyBatis-Plus 全局自动过滤）</li>
 *   <li>判断账号是否存在</li>
 *   <li>判断账号是否被禁用</li>
 *   <li>查询该用户的角色编码集合</li>
 *   <li>判断是否为超级管理员：若是，权限集合直接放通配符 {@code "*:*:*"}；否则按用户 ID 查询权限标识集合</li>
 *   <li>装配成 {@link LoginUser} 返回</li>
 * </ol>
 *
 * <p>本类只依赖 library-common 的接口，具体实现（AccountService/RoleService/PermissionService）
 * 由 Spring DI 在运行时从 library-admin 模块注入，避免反向依赖。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AccountService accountService;
    private final RoleService roleService;
    private final PermissionService permissionService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 按登录账号查询账号（逻辑删除自动过滤）
        AccountInfo account = accountService.loadByUsername(username);
        // 2. 账号不存在
        if (account == null) {
            log.warn("登录失败：账号不存在 username={}", username);
            throw new UsernameNotFoundException("账号不存在或密码错误");
        }
        // 3. 账号被禁用
        if (!UserStatus.ENABLED.getCode().equals(account.getStatus())) {
            log.warn("登录失败：账号已被禁用 username={}", username);
            throw new UsernameNotFoundException("账号已被禁用");
        }
        // 4. 查询角色编码集合
        Set<String> roles = roleService.getRoleCodes(account.getUserId());
        // 5. 判断超级管理员 -> 通配符；否则查权限集合
        Set<String> permissions;
        if (isSuperAdmin(roles)) {
            permissions = new HashSet<>();
            permissions.add(SecurityConstants.ALL_PERMISSION);
        } else {
            permissions = permissionService.getPermissionCodes(account.getUserId());
            if (permissions == null) {
                permissions = new HashSet<>();
            }
        }
        // 6. 装配 LoginUser
        return new LoginUser(
                account.getUserId(),
                account.getUsername(),
                account.getPassword(),
                UserStatus.ENABLED.getCode().equals(account.getStatus()),
                roles,
                permissions
        );
    }

    /**
     * 是否超级管理员：角色集合中包含 ROLE_ADMIN
     */
    private boolean isSuperAdmin(Set<String> roles) {
        return roles != null && roles.contains(SecurityConstants.ROLE_ADMIN);
    }
}