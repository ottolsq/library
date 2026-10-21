package com.libraryadmin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 菜单权限表（M目录/C菜单/F按钮 三层树）
 */
@Data
@TableName("sys_menu")
public class SysMenu implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 菜单ID
     */
    @TableId(value = "menu_id", type = IdType.AUTO)
    private Long menuId;

    /**
     * 菜单名称
     */
    private String menuName;

    /**
     * 父菜单ID，0为根节点
     */
    private Long parentId;

    /**
     * 类型：M目录 C菜单 F按钮
     */
    private String menuType;

    /**
     * 路由地址（C菜单用）
     */
    private String path;

    /**
     * 组件路径（C菜单用）
     */
    private String component;

    /**
     * 图标短名（前端映射表翻译）
     */
    private String icon;

    /**
     * 权限标识（F按钮用，格式 模块:功能:操作）
     */
    private String perms;

    /**
     * 同级排序，越小越靠前
     */
    private Integer sortOrder;
}
