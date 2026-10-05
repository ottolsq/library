package com.librarycommon.enums;

import lombok.Getter;

/**
 * 用户账号状态
 *
 * <p>约定：
 * <ul>
 *   <li>0 - 禁用</li>
 *   <li>1 - 正常</li>
 * </ul>
 */
@Getter
public enum UserStatus {

    DISABLED(0, "禁用"),
    ENABLED(1, "正常");

    private final Integer code;
    private final String desc;

    UserStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 判断账号是否可用
     */
    public boolean isEnabled() {
        return this == ENABLED;
    }
}