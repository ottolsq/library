package com.librarycommon.security;

import java.util.Set;

/**
 * 权限服务接口
 *
 * <p>定义在 library-common，由具体的业务模块提供实现。
 */
public interface PermissionService {

    /**
     * 根据用户 ID 查询权限标识集合（如 user:add）
     *
     * @param userId 用户主键ID
     * @return 权限标识集合，永不为 null（无权限返回空集合）
     */
    Set<String> getPermissionCodes(Long userId);
}