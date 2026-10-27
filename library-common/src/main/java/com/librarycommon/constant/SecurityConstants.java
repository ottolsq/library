package com.librarycommon.constant;

import java.util.Collections;
import java.util.LinkedHashMap;
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
     * Redis 改密时间前缀：值为改密时的 epoch 秒，
     * 用于拒绝签发时间早于改密时间的旧令牌
     */
    public static final String REDIS_PWD_CHANGED_PREFIX = "pwd:changed:";

    /**
     * Access Token 有效期：24 小时（毫秒）
     */
    public static final long ACCESS_TOKEN_EXPIRE = 24 * 60 * 60 * 1000L;

    /**
     * JWT 载荷：用户ID
     */
    public static final String JWT_USER_ID = "userId";

    /**
     * JWT 载荷：签发时间（秒，标准字段）
     */
    public static final String JWT_ISSUED_AT = "iat";

    /**
     * JWT 载荷：签发时间（毫秒，用于改密后精确判定旧令牌）
     */
    public static final String JWT_ISSUED_AT_MS = "iatMs";

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
     * 个人级接口前缀：这些接口不走 {@link #PERMISSION_RULES} 权限映射，
     * 任何已认证用户都可访问（登录、登出、当前用户信息、菜单树）。
     *
     * <p>注意 {@code /user/current} 会被 {@code /user/{id}} 的 Ant 模式误命中，
     * 所以授权管理器在比对权限表之前先排除这些前缀。
     */
    public static final String[] PERSONAL_PATHS = {
            "/user/login",
            "/user/logout",
            "/user/current",
            "/user/register",
            "/user/password",
            "/menu/"
    };

    /**
     * 请求级权限映射：HTTP 方法 + 路径 → 权限标识。
     *
     * <p>所有受保护接口的路径与权限对应关系只在此处维护，便于审计「哪些接口受保护」。
     * <p>路径支持 Ant 风格（{@code /book/**}），由 {@code RequestAuthorizationManager}
     * 通过 {@code AntPathMatcher} 解析。
     * <p>多条规则同时命中时，按「精确匹配优先 → 字面量字符更多优先 → 模式更长优先」
     * 选出最具体的一条，避免 {@code /user/list} 被 {@code /user/{id}} 抢匹配。
     * <p>未匹配到任何规则的请求视为无需鉴权，由管理器返回通过。
     */
    public static final Map<RequestMappingKey, String> PERMISSION_RULES;

    static {
        // 使用有序 Map 保证同等具体度时结果稳定，便于排查问题
        Map<RequestMappingKey, String> rules = new LinkedHashMap<>();

        // 用户模块（管理面接口；/user/current、/user/logout 等个人接口见 PERSONAL_PATHS）
        rules.put(new RequestMappingKey("GET", "/user/list"), "user:list");
        rules.put(new RequestMappingKey("GET", "/user/{id}"), "user:query");
        rules.put(new RequestMappingKey("POST", "/user"), "user:add");
        rules.put(new RequestMappingKey("PUT", "/user/{id}/status"), "user:status");
        rules.put(new RequestMappingKey("PUT", "/user/{id}/password"), "user:resetPwd");
        rules.put(new RequestMappingKey("PUT", "/user/{id}"), "user:edit");
        rules.put(new RequestMappingKey("DELETE", "/user/{id}"), "user:delete");

        // 读者管理
        rules.put(new RequestMappingKey("GET", "/reader/**"), "reader:query");
        rules.put(new RequestMappingKey("POST", "/reader"), "reader:add");
        rules.put(new RequestMappingKey("PUT", "/reader/**"), "reader:edit");
        rules.put(new RequestMappingKey("DELETE", "/reader/**"), "reader:remove");

        // 图书管理
        rules.put(new RequestMappingKey("GET", "/book/**"), "book:query");
        rules.put(new RequestMappingKey("POST", "/book"), "book:add");
        rules.put(new RequestMappingKey("PUT", "/book/**"), "book:edit");
        rules.put(new RequestMappingKey("DELETE", "/book/**"), "book:remove");

        // 借阅管理
        rules.put(new RequestMappingKey("GET", "/borrow/**"), "borrow:query");
        rules.put(new RequestMappingKey("POST", "/borrow"), "borrow:add");
        rules.put(new RequestMappingKey("PUT", "/borrow/**"), "borrow:edit");
        rules.put(new RequestMappingKey("DELETE", "/borrow/**"), "borrow:remove");

        PERMISSION_RULES = Collections.unmodifiableMap(rules);
    }

    /**
     * 判断 URI 是否属于个人级接口（不走 {@link #PERMISSION_RULES} 权限映射）。
     *
     * <p>使用「完全相等」或「前缀 + /」匹配，避免 {@code /user/currentX}
     * 这类不在白名单中的路径被误放行。
     */
    public static boolean isPersonalPath(String uri) {
        if (uri == null || uri.isBlank()) {
            return false;
        }
        for (String prefix : PERSONAL_PATHS) {
            if (prefix.endsWith("/")) {
                if (uri.startsWith(prefix)) {
                    return true;
                }
            } else if (uri.equals(prefix) || uri.startsWith(prefix + "/")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 请求映射键：HTTP 方法 + 路径（区分大小写）
     */
    public record RequestMappingKey(String method, String path) {
    }
}
