package com.libraryadmin.controller;

import com.libraryadmin.dto.ChangePasswordDTO;
import com.libraryadmin.service.AccountPasswordService;
import com.librarycommon.result.Result;
import com.librarycommon.security.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人中心接口
 *
 * <p>只处理当前登录用户自身的操作，路径在 {@code SecurityConstants.PERSONAL_PATHS} 中放行，
 * 不参与管理面权限映射。
 */
@RestController
@RequiredArgsConstructor
public class ProfileController {

    private final AccountPasswordService accountPasswordService;

    /**
     * 本人修改密码：校验原密码后写入 BCrypt 新密码
     */
    @PutMapping("/user/password")
    public Result<Void> changePassword(@AuthenticationPrincipal LoginUser loginUser,
                                       @Valid @RequestBody ChangePasswordDTO dto) {
        accountPasswordService.changePassword(
                loginUser.getUserId(), dto.oldPassword(), dto.newPassword());
        return Result.ok();
    }
}
