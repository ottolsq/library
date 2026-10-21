package com.libraryadmin.service;

import com.libraryadmin.dto.MenuTreeVO;

import java.util.List;

/**
 * 菜单服务
 */
public interface MenuService {

    /**
     * 查询当前登录用户可见的菜单树
     *
     * <p>超级管理员返回全量菜单树；普通角色按 sys_role_menu 绑定过滤。
     * 返回结果含 F 按钮节点（前端生成导航时过滤，按钮显隐用 perms）。
     *
     * @param userId      当前登录用户ID
     * @param isSuperAdmin 是否超级管理员
     * @return 菜单树（M/C/F 三层）
     */
    List<MenuTreeVO> getMenuTree(Long userId, boolean isSuperAdmin);
}
