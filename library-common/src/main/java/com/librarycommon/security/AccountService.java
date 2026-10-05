package com.librarycommon.security;

/**
 * 账号服务接口
 *
 * <p>定义在 library-common，由具体业务模块提供实现，避免反向依赖。
 */
public interface AccountService {

    /**
     * 按登录账号查询账号信息
     *
     * <p>返回的 AccountInfo 仅包含认证链必需的最小字段（id/username/password/status/role），
     * 避免跨层暴露实体。
     *
     * @param username 登录账号
     * @return 账号信息，账号不存在时返回 null
     */
    AccountInfo loadByUsername(String username);
}