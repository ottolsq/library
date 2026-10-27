package com.librarycommon.enums;

import lombok.Getter;

/**
 * 用户角色。
 *
 * <p>与 user.role 字段、sys_role_menu.role_id 的取值保持一致：
 * <ul>
 *   <li>0 - 管理员</li>
 *   <li>1 - 读者</li>
 * </ul>
 */
@Getter
public enum UserRole {

    ADMIN(0, "管理员"),
    READER(1, "读者");

    private final Integer code;
    private final String desc;

    UserRole(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据 code 反查枚举；找不到返回 null。
     */
    public static UserRole from(Integer code) {
        if (code == null) {
            return null;
        }
        for (UserRole role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        return null;
    }
}
