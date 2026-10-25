package com.libraryadmin.service;

import com.libraryadmin.entity.User;
import com.librarycommon.exception.BizException;
import com.libraryweb.util.RedisCacheUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 本人修改密码业务测试。
 */
@ExtendWith(MockitoExtension.class)
class AccountPasswordServiceTest {

    @Mock
    private UserService userService;

    private RecordingRedisCacheUtil redisCacheUtil;
    private PasswordEncoder passwordEncoder;
    private AccountPasswordService accountPasswordService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        redisCacheUtil = new RecordingRedisCacheUtil();
        accountPasswordService = new AccountPasswordService(
                userService, passwordEncoder, redisCacheUtil);
    }

    /**
     * 测试替身：只记录被删除的键，不依赖 Redis。
     *
     * <p>RedisCacheUtil 是具体类，当前 Byte Buddy 版本在 JDK 25 下无法内联 mock，
     * 这里用子类覆盖 delete，行为更直观。
     */
    private static class RecordingRedisCacheUtil extends RedisCacheUtil {
        private String deletedKey;
        private String changedAtKey;
        private Object changedAtValue;

        RecordingRedisCacheUtil() {
            super(null);
        }

        @Override
        public void delete(String key) {
            this.deletedKey = key;
        }

        @Override
        public void set(String key, Object value, long timeout, TimeUnit unit) {
            this.changedAtKey = key;
            this.changedAtValue = value;
        }
    }

    @Test
    @DisplayName("原密码不正确时拒绝修改且不写库")
    void changePassword_wrongOldPassword_throwsAndDoesNotUpdate() {
        when(userService.getById(1L)).thenReturn(userWithPassword("oldpass123"));

        assertThatThrownBy(() ->
                accountPasswordService.changePassword(1L, "wrongpass", "newpass123"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("原密码");

        verify(userService, never()).updateById(any());
    }

    @Test
    @DisplayName("原密码正确时写入 BCrypt 新密码")
    void changePassword_correctOldPassword_updatesEncodedPassword() {
        when(userService.getById(1L)).thenReturn(userWithPassword("oldpass123"));
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        accountPasswordService.changePassword(1L, "oldpass123", "newpass123");

        verify(userService).updateById(captor.capture());
        assertThat(passwordEncoder.matches("newpass123", captor.getValue().getPassword()))
                .isTrue();
        // 改密后立即删除 Redis 登录态，旧令牌随之失效
        assertThat(redisCacheUtil.deletedKey).isEqualTo("login:user:1");
        // 记录改密时间，登录时据此拒绝早于该时间签发的令牌
        assertThat(redisCacheUtil.changedAtKey).isEqualTo("pwd:changed:1");
        assertThat(((Number) redisCacheUtil.changedAtValue).longValue())
                .isCloseTo(System.currentTimeMillis(), within(2_000L));
    }

    @Test
    @DisplayName("新密码与原密码相同时拒绝修改")
    void changePassword_sameAsOld_throws() {
        when(userService.getById(1L)).thenReturn(userWithPassword("oldpass123"));

        assertThatThrownBy(() ->
                accountPasswordService.changePassword(1L, "oldpass123", "oldpass123"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不能与原密码相同");

        verify(userService, never()).updateById(any());
    }

    private User userWithPassword(String rawPassword) {
        User user = new User();
        user.setUserId(1L);
        user.setUsername("reader01");
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(1);
        user.setStatus(1);
        return user;
    }
}
