package com.libraryweb.security;

import com.librarycommon.constant.SecurityConstants;
import com.librarycommon.constant.SecurityConstants.RequestMappingKey;
import com.librarycommon.security.HasPermissionService;
import com.librarycommon.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.function.Supplier;

/**
 * 基于请求的授权管理器
 *
 * <p>挂在 {@code .anyRequest().access(...)} 上，
 * 与方法注解 {@code @PreAuthorize("@ss.hasPermission('xxx')")} 共用 {@link HasPermissionService}，
 * 保证路径级与方法级授权走同一份规则。
 *
 * <p>判断分四层：
 * <ol>
 *   <li>从请求上下文取出请求方法、路径，并取出当前认证信息</li>
 *   <li>认证信息为空 / 未通过认证 / 主体是匿名用户 → 拒绝</li>
 *   <li>当前用户是超级管理员 → 直接通过，不再比对权限</li>
 *   <li>按方法 + 路径在 {@link SecurityConstants#PERMISSION_RULES} 中解析所需权限；
 *       未配置 → 通过；已配置 → 调用 {@link HasPermissionService#hasPermission(String)} 比对，
 *       命中通过，未命中拒绝并告警日志</li>
 * </ol>
 *
 * <p>路径与权限的对应关系集中在 {@link SecurityConstants#PERMISSION_RULES} 维护，
 * 便于审阅「哪些接口受保护」。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequestAuthorizationManager
        implements AuthorizationManager<RequestAuthorizationContext> {

    private final HasPermissionService hasPermissionService;

    /**
     * Ant 风格路径匹配器（处理 {@code /book/**}、{@code /reader/**} 等通配）
     */
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public AuthorizationDecision check(
            Supplier<Authentication> authentication,
            RequestAuthorizationContext context) {

        // 第 1 层：从请求上下文取出方法、路径与认证信息
        String method = context.getRequest().getMethod();
        String uri = context.getRequest().getRequestURI();
        Authentication auth = authentication.get();

        // 第 2 层：认证为空 / 未通过 / 匿名 → 拒绝
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return deny("未通过认证或主体为匿名");
        }
        Object principal = auth.getPrincipal();
        if (!(principal instanceof LoginUser loginUser)) {
            return deny("主体类型不是 LoginUser");
        }

        // 第 3 层：超级管理员 → 直接通过
        if (loginUser.isSuperAdmin()) {
            return grant("超级管理员放行");
        }

        // 第 4 层：按方法 + 路径解析所需权限
        String requiredPermission = resolvePermission(method, uri);
        if (requiredPermission == null) {
            return grant("未配置权限要求，默认放行");
        }
        if (hasPermissionService.hasPermission(requiredPermission)) {
            return grant("权限命中: " + requiredPermission);
        }
        log.warn("权限拒绝 method={} uri={} required={} userId={} username={}",
                method, uri, requiredPermission, loginUser.getUserId(), loginUser.getUsername());
        return deny("权限不足: " + requiredPermission);
    }

    /**
     * 按 HTTP 方法 + 路径在 {@link SecurityConstants#PERMISSION_RULES} 中匹配所需权限
     *
     * @return 所需权限标识；未配置返回 {@code null}（表示默认放行）
     */
    private String resolvePermission(String method, String uri) {
        // 先按精确 method + path 命中
        for (var entry : SecurityConstants.PERMISSION_RULES.entrySet()) {
            RequestMappingKey key = entry.getKey();
            if (!method.equals(key.method())) {
                continue;
            }
            if (PATH_MATCHER.match(key.path(), uri)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static AuthorizationDecision grant(String reason) {
        log.debug("授权通过: {}", reason);
        return new AuthorizationDecision(true);
    }

    private static AuthorizationDecision deny(String reason) {
        log.debug("授权拒绝: {}", reason);
        return new AuthorizationDecision(false);
    }
}