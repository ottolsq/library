package com.libraryadmin.service;

import com.libraryadmin.entity.User;
import com.librarycommon.exception.BizException;
import com.librarycommon.result.ResultCode;
import com.libraryweb.service.LoginStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final LoginStateService loginStateService;

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
        // 删除登录态并记录改密时间：当前令牌立即失效，
        // 用户重新登录后旧令牌也会因“签发时间早于改密时间”被拒绝
        loginStateService.evictAfterPasswordChangeAfterCommit(userId);
    }
}
