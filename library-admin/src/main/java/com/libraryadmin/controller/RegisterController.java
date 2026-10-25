package com.libraryadmin.controller;

import com.libraryadmin.dto.RegisterDTO;
import com.libraryadmin.service.RegisterService;
import com.librarycommon.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 读者自助注册接口。
 *
 * <p>该路径在 SecurityConfig 白名单中，未登录也可访问；
 * 注册角色固定为读者，不能创建管理员账号。
 */
@RestController
@RequiredArgsConstructor
public class RegisterController {

    private final RegisterService registerService;

    @PostMapping("/user/register")
    public Result<Long> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.ok(registerService.register(dto));
    }
}
