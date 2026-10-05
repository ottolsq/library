package com.libraryadmin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.libraryadmin.entity.User;
import com.libraryadmin.mapper.UserMapper;
import com.libraryadmin.service.UserService;
import com.librarycommon.security.AccountInfo;
import com.librarycommon.security.AccountService;
import com.librarycommon.security.PermissionService;
import com.librarycommon.security.RoleService;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * 系统用户 Service 实现
 *
 * <p>同时实现 library-common 的 {@link AccountService} / {@link RoleService} / {@link PermissionService}，
 * 由 Spring DI 把 admin 的实现注入到 library-web 的 UserDetailsServiceImpl 中，
 * 避免反向依赖。
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService, AccountService, RoleService, PermissionService {

    /* ================== UserService ================== */

    @Override
    public User getByUsername(String username) {
        // MyBatis-Plus 全局配置已开启逻辑删除字段 deleted，这里 baseMapper 的查询会自动过滤 deleted=1
        return baseMapper.selectByUsername(username);
    }

    /* ================== AccountService ================== */

    @Override
    public AccountInfo loadByUsername(String username) {
        User user = getByUsername(username);
        if (user == null) {
            return null;
        }
        return new AccountInfo(
                user.getUserId(),
                user.getUsername(),
                user.getPassword(),
                user.getStatus(),
                user.getRole()
        );
    }

    /* ================== RoleService ================== */

    @Override
    public Set<String> getRoleCodes(Long userId) {
        // 当前未引入角色表与关联表，先以 user.role 单字段映射为 ROLE_ADMIN / ROLE_READER
        User user = baseMapper.selectById(userId);
        if (user == null || user.getRole() == null) {
            return new HashSet<>();
        }
        Set<String> roles = new HashSet<>();
        if (user.getRole() == 0) {
            roles.add("ROLE_ADMIN");
        } else if (user.getRole() == 1) {
            roles.add("ROLE_READER");
        }
        return roles;
    }

    /* ================== PermissionService ================== */

    @Override
    public Set<String> getPermissionCodes(Long userId) {
        // 当前未引入权限表与关联表，返回空集合（用户详情空集合可在请求链路中安全流转）
        return new HashSet<>();
    }
}