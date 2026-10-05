package com.libraryweb.controller;

import com.librarycommon.result.Result;
import com.librarycommon.security.LoginUser;
import com.libraryweb.dto.CurrentUserVO;
import com.libraryweb.dto.LoginDTO;
import com.libraryweb.dto.LoginVO;
import com.libraryweb.service.LoginService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户认证相关接口
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final LoginService loginService;

    /**
     * 登录
     *
     * @param loginDTO 登录参数
     * @return 登录响应（token + 有效期 + 用户基本信息）
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        return Result.ok(loginService.login(loginDTO));
    }

    /**
     * 当前登录用户信息
     *
     * <p>从 SecurityContext 中取出由 JwtAuthenticationFilter 写入的 {@link LoginUser}。
     * <p>未携带有效 token 时由 JwtAuthenticationFilter 放行后，
     * 授权环节会通过 {@code AuthenticationEntryPoint} 返回 401。
     */
    @GetMapping("/current")
    public Result<CurrentUserVO> current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        CurrentUserVO vo = new CurrentUserVO(
                loginUser.getUserId(),
                loginUser.getUsername(),
                loginUser.isEnabled(),
                loginUser.getRoles(),
                loginUser.getPermissions(),
                loginUser.isSuperAdmin()
        );
        return Result.ok(vo);
    }

    /**
     * 退出登录
     *
     * <p>删除 Redis 中保存的登录态、清空安全上下文。
     * <p>退出后该 token 立即失效，再访问受保护接口会被 {@code AuthenticationEntryPoint} 拦截返回 401。
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        loginService.logout();
        return Result.ok();
    }
}