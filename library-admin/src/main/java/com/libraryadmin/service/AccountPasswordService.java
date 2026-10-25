package com.libraryadmin.service;

import com.libraryadmin.entity.User;
import com.librarycommon.constant.SecurityConstants;
import com.librarycommon.exception.BizException;
import com.librarycommon.result.ResultCode;
import com.libraryweb.util.RedisCacheUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * 本人修改密码业务。
 *
 * <p>与管理员重置密码不同，此处必须先校验原密码，避免令牌泄露后被直接改密。
 */
@Service
@RequiredArgsConstructor
public class AccountPasswordService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final RedisCacheUtil redisCacheUtil;

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BizException(ResultCode.PARAM_INVALID, "原密码不正确");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BizException(ResultCode.PARAM_INVALID, "新密码不能与原密码相同");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userService.updateById(user);
        // 改密后立即删除 Redis 登录态，旧令牌即使未过期也无法再取到身份信息
        redisCacheUtil.delete(SecurityConstants.REDIS_LOGIN_USER_PREFIX + userId);
        // 记录改密时间：即使 attacker 在用户重新登录后复用旧令牌，
        // 过滤器也会因“签发时间早于改密时间”而拒绝
        redisCacheUtil.set(
                SecurityConstants.REDIS_PWD_CHANGED_PREFIX + userId,
                System.currentTimeMillis(),
                SecurityConstants.ACCESS_TOKEN_EXPIRE,
                TimeUnit.MILLISECONDS);
    }
}
