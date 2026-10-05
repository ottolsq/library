package com.librarycommon.constant;

import java.util.Map;

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

    /**
     * 请求级权限映射：HTTP 方法 + 路径 → 权限标识。
     *
     * <p>所有受保护接口的路径与权限对应关系只在此处维护，便于审计「哪些接口受保护」。
     * <p>路径支持 Ant 风格（{@code /book/**}），由 {@code RequestAuthorizationManager}
     * 通过 {@code AntPathMatcher} 解析；多条规则同时命中时取最长路径前缀。
     * <p>未匹配到任何规则的请求视为无需鉴权，由管理器返回通过。
     */
    public static final Map<RequestMappingKey, String> PERMISSION_RULES = Map.ofEntries(
            // 用户模块（管理面接口）
            Map.entry(new RequestMappingKey("GET", "/user/list"), "user:list"),
            Map.entry(new RequestMappingKey("GET", "/user/{id}"), "user:query"),
            Map.entry(new RequestMappingKey("POST", "/user"), "user:add"),
            Map.entry(new RequestMappingKey("PUT", "/user/{id}"), "user:edit"),
            Map.entry(new RequestMappingKey("PUT", "/user/{id}/status"), "user:status"),
            Map.entry(new RequestMappingKey("PUT", "/user/{id}/password"), "user:resetPwd"),
            Map.entry(new RequestMappingKey("DELETE", "/user/{id}"), "user:delete"),

            // 个人 / 当前用户
            Map.entry(new RequestMappingKey("GET", "/user/current"), "user:query"),
            Map.entry(new RequestMappingKey("POST", "/user/logout"), "user:logout"),

            // 读者管理
            Map.entry(new RequestMappingKey("GET", "/reader/**"), "reader:query"),
            Map.entry(new RequestMappingKey("POST", "/reader"), "reader:add"),
            Map.entry(new RequestMappingKey("PUT", "/reader/**"), "reader:edit"),
            Map.entry(new RequestMappingKey("DELETE", "/reader/**"), "reader:remove"),

            // 图书管理
            Map.entry(new RequestMappingKey("GET", "/book/**"), "book:query"),
            Map.entry(new RequestMappingKey("POST", "/book"), "book:add"),
            Map.entry(new RequestMappingKey("PUT", "/book/**"), "book:edit"),
            Map.entry(new RequestMappingKey("DELETE", "/book/**"), "book:remove"),

            // 借阅管理
            Map.entry(new RequestMappingKey("GET", "/borrow/**"), "borrow:query"),
            Map.entry(new RequestMappingKey("POST", "/borrow"), "borrow:add"),
            Map.entry(new RequestMappingKey("PUT", "/borrow/**"), "borrow:edit"),
            Map.entry(new RequestMappingKey("DELETE", "/borrow/**"), "borrow:remove")
    );

    /**
     * 请求映射键：HTTP 方法 + 路径（区分大小写）
     */
    public record RequestMappingKey(String method, String path) {
    }
}
