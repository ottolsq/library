package com.librarycommon.constant;

/**
 * 安全相关常量
 */
public final class SecurityConstants {

    private SecurityConstants() {
    }

    /**
     * 请求头名称
     */
    public static final String TOKEN_HEADER = "Authorization";

    /**
     * Token 前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * JWT 负载中存放登录用户的 key
     */
    public static final String LOGIN_USER_KEY = "loginUser";

    /**
     * Redis 登录用户前缀
     */
    public static final String REDIS_LOGIN_USER_PREFIX = "login:user:";

    /**
     * Access Token 有效期：24 小时（毫秒）
     */
    public static final long ACCESS_TOKEN_EXPIRE = 24 * 60 * 60 * 1000L;

    /**
     * JWT 签名密钥（仅用于本地 dev，生产环境必须通过配置覆盖）
     */
    public static final String JWT_SECRET = "library-dev-secret-key-2026-please-change-in-prod";

    /**
     * JWT 载荷：用户ID
     */
    public static final String JWT_USER_ID = "userId";

    /**
     * JWT 载荷：签发时间
     */
    public static final String JWT_ISSUED_AT = "iat";

    /**
     * JWT 载荷：过期时间
     */
    public static final String JWT_EXPIRATION = "exp";

    /**
     * 匿名用户角色
     */
    public static final String ROLE_ANONYMOUS = "ROLE_ANONYMOUS";

    /**
     * 管理员角色
     */
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    /**
     * 读者角色
     */
    public static final String ROLE_READER = "ROLE_READER";

    /**
     * 超级管理员权限通配符：拥有该权限即可访问所有受控资源
     */
    public static final String ALL_PERMISSION = "*:*:*";

    /**
     * 资源/操作/权限分隔符
     */
    public static final String PERMISSION_DELIMITER = ":";
}
