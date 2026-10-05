package com.librarycommon.security;

import java.util.Set;

/**
 * 角色服务接口
 *
 * <p>定义在 library-common，由具体的业务模块（如 library-admin）提供实现，
 * 避免 library-web 反向依赖 library-admin。
 */
public interface RoleService {

    /**
     * 根据用户 ID 查询角色编码集合（ROLE_xxx）
     *
     * @param userId 用户主键ID
     * @return 角色编码集合，永不为 null（空用户返回空集合）
     */
    Set<String> getRoleCodes(Long userId);
}