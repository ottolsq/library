package com.libraryadmin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.libraryadmin.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 菜单 Mapper
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 按角色查询已绑定的菜单
     *
     * @param roleId 角色ID：0管理员，1读者
     * @return 该角色绑定的菜单列表
     */
    @Select("""
            SELECT m.*
            FROM sys_menu m
            JOIN sys_role_menu rm ON rm.menu_id = m.menu_id
            WHERE rm.role_id = #{roleId}
            ORDER BY m.sort_order, m.menu_id
            """)
    List<SysMenu> selectByRoleId(Integer roleId);

    /**
     * 查用户角色数值（与 user.role 对齐：0管理员，1读者）
     *
     * @param userId 用户主键ID
     * @return 角色数值；用户不存在返回 null
     */
    @Select("SELECT role FROM user WHERE user_id = #{userId}")
    Integer selectUserRole(Long userId);

    /**
     * 按角色查询绑定的权限标识
     *
     * <p>同时取 C 菜单的查询权限（如 book:query）与 F 按钮的操作权限（如 book:add）。
     * 只取 F 节点会导致普通角色能拿到操作权限、却无法调用列表查询接口。
     *
     * @param roleId 角色ID：0管理员，1读者
     * @return 权限标识列表（去重、去空）
     */
    @Select("""
            SELECT DISTINCT m.perms
            FROM sys_menu m
            JOIN sys_role_menu rm ON rm.menu_id = m.menu_id
            WHERE rm.role_id = #{roleId}
              AND m.menu_type IN ('C', 'F')
              AND m.perms IS NOT NULL
            """)
    List<String> selectPermsByRoleId(Integer roleId);
}
