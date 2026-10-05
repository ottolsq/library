package com.libraryweb.service;

import com.librarycommon.constant.SecurityConstants;
import com.librarycommon.security.LoginUser;
import com.librarycommon.util.JwtUtil;
import com.libraryweb.dto.LoginDTO;
import com.libraryweb.dto.LoginVO;
import com.libraryweb.util.RedisCacheUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 登录业务
 *
 * <p>login 装配流程（七步）：
 * <ol>
 *   <li>接收账号密码参数</li>
 *   <li>构造未认证的认证请求对象，把账号密码装进去</li>
 *   <li>调用认证管理器完成认证（框架会自动调用 UserDetailsService）</li>
 *   <li>从认证结果中取出已装配角色与权限的用户详情对象</li>
 *   <li>把用户详情对象以用户 ID 为键写入 Redis，过期时间与令牌有效期一致</li>
 *   <li>签发令牌，载荷中只放用户 ID</li>
 *   <li>组装响应：令牌、有效期秒数、用户信息</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService {

    private final AuthenticationManager authenticationManager;
    private final RedisCacheUtil redisCacheUtil;

    /**
     * 登录
     *
     * <p>认证失败统一抛出 {@link BadCredentialsException}（提示文案统一为「账号或密码错误」），
     * 不区分账号不存在与密码错误，防止通过提示差异枚举账号。
     */
    public LoginVO login(LoginDTO loginDTO) {
        // 1. 接收账号密码（参数由 Controller 通过 @Valid 校验）
        // 2. 构造未认证的认证请求对象
        UsernamePasswordAuthenticationToken authRequest =
                new UsernamePasswordAuthenticationToken(loginDTO.username(), loginDTO.password());
        // 3. 调用认证管理器完成认证
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(authRequest);
        } catch (AuthenticationException e) {
            // 4*. 失败统一转成 BadCredentialsException，文案不区分账号/密码
            throw new BadCredentialsException("账号或密码错误");
        }
        // 4. 取出已装配角色与权限的用户详情
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        Long userId = loginUser.getUserId();
        // 5. 写入 Redis（key = login:user:{userId}，TTL 与 token 一致）
        String redisKey = SecurityConstants.REDIS_LOGIN_USER_PREFIX + userId;
        redisCacheUtil.set(redisKey, loginUser,
                SecurityConstants.ACCESS_TOKEN_EXPIRE, TimeUnit.MILLISECONDS);
        // 6. 签发令牌（载荷仅含 userId）
        String token = JwtUtil.createToken(userId);
        // 7. 组装响应
        Set<String> roles = loginUser.getRoles();
        Set<String> permissions = loginUser.getPermissions();
        log.info("登录成功 userId={} username={}", userId, loginUser.getUsername());
        return new LoginVO(
                token,
                SecurityConstants.ACCESS_TOKEN_EXPIRE / 1000,
                loginUser.getUsername(),
                loginUser.getUsername(),
                roles,
                permissions
        );
    }

    /**
     * 退出登录
     *
     * <p>三步：
     * <ol>
     *   <li>从安全上下文取出当前已认证的 {@link LoginUser}</li>
     *   <li>按登录时相同的规则拼出 Redis 键（{@code login:user:{userId}}）并删除</li>
     *   <li>清空安全上下文</li>
     * </ol>
     *
     * <p>由于 {@code JwtAuthenticationFilter} 每次请求都会从 Redis 加载用户信息，
     * 缓存被删除后该令牌即使签名有效、时间未到也取不到身份信息，等同于立即失效。
     */
    public void logout() {
        // 1. 从安全上下文取出当前已认证的用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            // 已退出或未登录，幂等返回成功
            SecurityContextHolder.clearContext();
            return;
        }
        Long userId = loginUser.getUserId();
        // 2. 用与登录时完全一致的规则拼 Redis 键并删除
        String redisKey = SecurityConstants.REDIS_LOGIN_USER_PREFIX + userId;
        redisCacheUtil.delete(redisKey);
        // 3. 清空安全上下文
        SecurityContextHolder.clearContext();
        log.info("退出登录 userId={} username={}", userId, loginUser.getUsername());
    }
}