package com.libraryweb.filter;

import cn.hutool.core.util.StrUtil;
import com.librarycommon.constant.SecurityConstants;
import com.librarycommon.security.LoginUser;
import com.librarycommon.util.JwtUtil;
import com.libraryweb.util.RedisCacheUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * JWT 认证过滤器
 *
 * <p>从请求头解析 token，校验后从 Redis 加载完整 {@link LoginUser} 写入 SecurityContext。
 * <p>token 中只携带用户 ID，username/role/permissions 等详情从 Redis 读取。
 *
 * <p><b>设计原则</b>：
 * <ol>
 *   <li>认证失败（无 token / token 无效 / Redis 缺失 / 账号已停用）一律<b>放行</b>，
 *       不抛异常也不写 401，由后续授权环节统一处理；
 *       避免 JWT 过滤器与异常处理链产生耦合。</li>
 *   <li>判断 SecurityContext 是否已处理时，不能只看 authentication == null，
 *       因为 Spring Security 在过滤器链前段会放入 {@link AnonymousAuthenticationToken}。
 *       这里用 {@code instanceof AnonymousAuthenticationToken} 显式排除匿名身份。</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final RedisCacheUtil redisCacheUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 0. 已认证（非匿名）则直接放行，避免重复解析 token
        if (isAlreadyAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. 从请求头取出令牌（兼容带前缀与不带前缀两种写法）
        String token = resolveToken(request);

        // 2. 无令牌直接放行
        if (StrUtil.isBlank(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. 校验令牌签名与有效期
        if (!JwtUtil.validateToken(token)) {
            log.warn("token 校验失败: uri={}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        // 4. 解析出用户 ID
        Long userId = JwtUtil.parseUserId(token);
        if (userId == null) {
            log.warn("token 解析 userId 失败: uri={}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        // 5. 用该 ID 从 Redis 取回用户详情
        String redisKey = SecurityConstants.REDIS_LOGIN_USER_PREFIX + userId;
        LoginUser loginUser = redisCacheUtil.get(redisKey, LoginUser.class);

        // 6. 缓存中不存在则放行
        if (loginUser == null) {
            log.warn("Redis 中未找到登录用户: userId={}, uri={}", userId, request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        // 7. 二次校验账号状态（登录后被禁用的极端场景）
        if (Boolean.FALSE.equals(loginUser.isEnabled())) {
            log.warn("账号已被禁用: userId={}, uri={}", userId, request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        // 8. 构造已认证的认证对象，写入安全上下文，放行请求
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        authentication.setDetails(request);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 续期 Redis 缓存（活跃用户保持登录态）
        redisCacheUtil.renew(redisKey, SecurityConstants.ACCESS_TOKEN_EXPIRE, TimeUnit.MILLISECONDS);

        filterChain.doFilter(request, response);
    }

    /**
     * 判断 SecurityContext 是否已经放入非匿名认证对象。
     *
     * <p>Spring Security 在过滤器链前段会写入 {@link AnonymousAuthenticationToken}，
     * 所以仅判断 authentication == null 不足以识别"本过滤器已处理过"的状态，
     * 这里必须显式排除匿名身份。
     */
    private boolean isAlreadyAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return !(authentication instanceof AnonymousAuthenticationToken);
    }

    /**
     * 从请求头解析 token（兼容带 {@code Bearer } 前缀与裸 token 两种写法）。
     */
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(SecurityConstants.TOKEN_HEADER);
        return JwtUtil.stripTokenPrefix(bearer);
    }
}
