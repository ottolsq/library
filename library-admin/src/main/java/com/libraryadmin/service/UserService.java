package com.libraryadmin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.libraryadmin.entity.User;

import java.util.Set;

/**
 * 系统用户 Service
 */
public interface UserService extends IService<User> {

    /**
     * 根据登录账号查询用户
     *
     * <p>自动过滤逻辑删除字段为 deleted=1 的记录。
     */
    User getByUsername(String username);

    /**
     * 查询用户拥有的角色编码集合（ROLE_xxx）
     */
    Set<String> getRoleCodes(Long userId);

    /**
     * 查询用户拥有的权限标识集合（如 user:add）
     */
    Set<String> getPermissionCodes(Long userId);
}