package com.libraryweb.security;

import com.librarycommon.constant.SecurityConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 路径级权限解析测试。
 *
 * <p>重点验证：多条 Ant 规则同时命中时，必须选择最具体的一条，
 * 不能让 {@code /user/{id}} 抢走 {@code /user/list} 的权限。
 */
class RequestAuthorizationManagerTest {

    @Test
    @DisplayName("GET /user/list 使用 user:list，而不是被 /user/{id} 匹配")
    void resolvePermission_userList_prefersLiteralRule() {
        assertThat(RequestAuthorizationManager.resolvePermission("GET", "/user/list"))
                .isEqualTo("user:list");
    }

    @Test
    @DisplayName("GET /user/{id} 使用 user:query")
    void resolvePermission_userDetail_usesQueryRule() {
        assertThat(RequestAuthorizationManager.resolvePermission("GET", "/user/12"))
                .isEqualTo("user:query");
    }

    @Test
    @DisplayName("PUT /user/{id}/status 使用 user:status")
    void resolvePermission_userStatus_usesStatusRule() {
        assertThat(RequestAuthorizationManager.resolvePermission("PUT", "/user/12/status"))
                .isEqualTo("user:status");
    }

    @Test
    @DisplayName("PUT /user/{id}/password 使用 user:resetPwd")
    void resolvePermission_userPassword_usesResetRule() {
        assertThat(RequestAuthorizationManager.resolvePermission("PUT", "/user/12/password"))
                .isEqualTo("user:resetPwd");
    }

    @Test
    @DisplayName("未配置权限的路径返回 null（默认放行）")
    void resolvePermission_unknownPath_returnsNull() {
        assertThat(RequestAuthorizationManager.resolvePermission("GET", "/unknown"))
                .isNull();
    }

    @Test
    @DisplayName("个人级接口按精确路径或前缀匹配，不误放行相似路径")
    void isPersonalPath_doesNotMatchLookalike() {
        assertThat(SecurityConstants.isPersonalPath("/user/current")).isTrue();
        assertThat(SecurityConstants.isPersonalPath("/user/current-x")).isFalse();
        assertThat(SecurityConstants.isPersonalPath("/menu/tree")).isTrue();
    }
}
