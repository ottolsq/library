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
     *
     * @param role     角色筛选（0 管理员 / 1 读者）；为 null 时不过滤
     * @param unlinked 是否只返回尚未关联读者档案的账号（用于新增读者下拉）
     */
    IPage<User> pageUsers(long pageNum, long pageSize, Integer role, Boolean unlinked);

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
     *
     * @param currentUserId 当前登录用户ID，用于阻止自我降级
     */
    void updateUser(Long userId, UpdateUserDTO dto, Long currentUserId);

    /**
     * 启用或禁用账号
     *
     * @param currentUserId 当前登录用户ID，用于阻止自我禁用
     */
    void updateStatus(Long userId, Integer status, Long currentUserId);

    /**
     * 重置密码（明文入库前 BCrypt 哈希）
     */
    void resetPassword(Long userId, String newPassword);

    /**
     * 删除用户（逻辑删除）
     *
     * @param currentUserId 当前登录用户ID，用于阻止自我删除
     */
    void deleteUser(Long userId, Long currentUserId);
}