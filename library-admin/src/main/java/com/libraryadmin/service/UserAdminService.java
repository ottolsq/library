package com.libraryadmin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.libraryadmin.dto.CreateUserDTO;
import com.libraryadmin.dto.UpdateUserDTO;
import com.libraryadmin.entity.User;

/**
 * 系统用户管理服务
 */
public interface UserAdminService {

    /**
     * 分页查询用户列表
     */
    IPage<User> pageUsers(long pageNum, long pageSize);

    /**
     * 查询用户详情
     */
    User getById(Long userId);

    /**
     * 新增用户
     */
    Long createUser(CreateUserDTO dto);

    /**
     * 修改用户资料
     */
    void updateUser(Long userId, UpdateUserDTO dto);

    /**
     * 启用或禁用账号
     */
    void updateStatus(Long userId, Integer status);

    /**
     * 重置密码（明文入库前 BCrypt 哈希）
     */
    void resetPassword(Long userId, String newPassword);

    /**
     * 删除用户（逻辑删除）
     */
    void deleteUser(Long userId);
}