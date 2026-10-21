package com.libraryadmin.controller;

import com.libraryadmin.dto.MenuTreeVO;
import com.libraryadmin.service.MenuService;
import com.librarycommon.result.Result;
import com.librarycommon.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 菜单接口
 *
 * <p>登录后由前端单独拉取（登录接口只返回令牌、用户信息与权限，不含菜单）。
 */
@RestController
@RequestMapping("/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    /**
     * 查询当前登录用户可见的菜单树
     */
    @GetMapping("/tree")
    public Result<List<MenuTreeVO>> tree(@AuthenticationPrincipal LoginUser loginUser) {
        List<MenuTreeVO> tree = menuService.getMenuTree(
                loginUser.getUserId(),
                loginUser.isSuperAdmin());
        return Result.ok(tree);
    }
}
