package com.libraryadmin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.libraryadmin.dto.MenuTreeVO;
import com.libraryadmin.entity.SysMenu;
import com.libraryadmin.mapper.SysMenuMapper;
import com.libraryadmin.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单服务实现
 *
 * <p>菜单数据全量在 sys_menu 表维护，角色可见范围在 sys_role_menu 维护；
 * 建树时保持 sort_order 排序，F 按钮节点挂在所属菜单的 children 下原样返回。
 */
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final SysMenuMapper sysMenuMapper;

    @Override
    public List<MenuTreeVO> getMenuTree(Long userId, boolean isSuperAdmin) {
        // 超管取全量；普通角色按绑定过滤
        List<SysMenu> menus = isSuperAdmin
                ? sysMenuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                        .orderByAsc(SysMenu::getSortOrder)
                        .orderByAsc(SysMenu::getMenuId))
                : sysMenuMapper.selectByRoleId(resolveRoleId(userId));
        return buildTree(menus);
    }

    /**
     * 按用户角色取绑定用的 role_id（与 user.role 单字段模型对齐：0管理员，1读者）
     */
    private Integer resolveRoleId(Long userId) {
        // sys_role_menu.role_id 与 user.role 数值语义一致，直接复用
        // 通过注入的 RoleService 已在登录链路使用；这里按 userId 反查一次角色
        return userRole(userId);
    }

    /**
     * 平铺菜单列表 → 三层树
     *
     * <p>parent_id = 0 为根；先按 menuId 分组，再从根递归挂 children。
     */
    private List<MenuTreeVO> buildTree(List<SysMenu> menus) {
        Map<Long, List<SysMenu>> byParent = menus.stream()
                .collect(Collectors.groupingBy(SysMenu::getParentId));
        // 每层按 sort_order、menuId 稳定排序
        byParent.values().forEach(list ->
                list.sort(Comparator.comparing(SysMenu::getSortOrder)
                        .thenComparing(SysMenu::getMenuId)));
        return menus.stream()
                .filter(m -> m.getParentId() == 0L)
                .map(m -> toVO(m, byParent))
                .toList();
    }

    private MenuTreeVO toVO(SysMenu menu, Map<Long, List<SysMenu>> byParent) {
        List<MenuTreeVO> children = byParent.getOrDefault(menu.getMenuId(), List.of()).stream()
                .map(m -> toVO(m, byParent))
                .toList();
        return new MenuTreeVO(
                menu.getMenuId(),
                menu.getMenuName(),
                menu.getMenuType(),
                menu.getPath(),
                menu.getComponent(),
                menu.getIcon(),
                menu.getPerms(),
                children.isEmpty() ? new ArrayList<>() : children
        );
    }

    /**
     * 查用户角色数值（0管理员/1读者）；查不到按读者（最小可见范围）处理
     */
    private Integer userRole(Long userId) {
        return sysMenuMapper.selectUserRole(userId);
    }
}
