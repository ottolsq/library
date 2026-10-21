package com.libraryadmin.dto;

import java.util.List;

/**
 * 菜单树节点 VO
 *
 * <p>M目录/C菜单/F按钮统一用此结构；F 按钮节点只在 permissions 里生效，
 * 前端生成导航时会过滤掉。
 *
 * @param menuId    菜单ID
 * @param menuName  菜单名称
 * @param menuType  类型：M目录 C菜单 F按钮
 * @param path      路由地址（C菜单用）
 * @param component 组件路径（C菜单用）
 * @param icon      图标短名
 * @param perms     权限标识（F按钮用）
 * @param children  子节点
 */
public record MenuTreeVO(
        Long menuId,
        String menuName,
        String menuType,
        String path,
        String component,
        String icon,
        String perms,
        List<MenuTreeVO> children
) {
}
