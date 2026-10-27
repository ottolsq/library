package com.libraryadmin.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.libraryadmin.dto.UpdateUserDTO;
import com.libraryadmin.entity.Reader;
import com.libraryadmin.entity.User;
import com.libraryadmin.service.impl.UserAdminServiceImpl;
import com.librarycommon.exception.BizException;
import com.librarycommon.result.ResultCode;
import com.libraryweb.service.LoginStateService;
import com.libraryweb.util.RedisCacheUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 系统用户管理测试：自我保护、最后管理员保护、登录态失效与删除墓碑。
 */
@ExtendWith(MockitoExtension.class)
class UserAdminServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private ReaderService readerService;

    private RecordingRedisCacheUtil redisCacheUtil;
    private PasswordEncoder passwordEncoder;
    private UserAdminServiceImpl userAdminService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        redisCacheUtil = new RecordingRedisCacheUtil();
        userAdminService = new UserAdminServiceImpl(
                userService,
                passwordEncoder,
                new LoginStateService(redisCacheUtil),
                readerService);
    }

    @Test
    @DisplayName("不能禁用当前登录账号")
    void updateStatus_selfDisable_throws() {
        when(userService.getById(1L)).thenReturn(user(1L, 0, 1, "admin"));

        assertThatThrownBy(() -> userAdminService.updateStatus(1L, 0, 1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不能禁用当前登录账号");

        verify(userService, never()).updateById(any());
    }

    @Test
    @DisplayName("不能删除当前登录账号")
    void deleteUser_self_throws() {
        when(userService.getById(1L)).thenReturn(user(1L, 0, 1, "admin"));

        assertThatThrownBy(() -> userAdminService.deleteUser(1L, 1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不能删除当前登录账号");

        verify(userService, never()).removeById(1L);
    }

    @Test
    @DisplayName("不能修改当前登录账号的角色")
    void updateUser_selfRoleChange_throws() {
        when(userService.getById(1L)).thenReturn(user(1L, 0, 1, "admin"));

        assertThatThrownBy(() ->
                userAdminService.updateUser(1L, new UpdateUserDTO(null, 1, null), 1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不能修改当前登录账号的角色");

        verify(userService, never()).updateById(any());
    }

    @Test
    @DisplayName("不能禁用最后一个启用的管理员")
    void updateStatus_lastEnabledAdmin_throws() {
        when(userService.getById(1L)).thenReturn(user(1L, 0, 1, "admin"));
        when(userService.list(ArgumentMatchers.<Wrapper<User>>any()))
                .thenReturn(List.of(user(1L, 0, 1, "admin")));

        assertThatThrownBy(() -> userAdminService.updateStatus(1L, 0, 2L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("最后一个启用的管理员");

        verify(userService, never()).updateById(any());
    }

    @Test
    @DisplayName("禁用其他账号后立即清理其 Redis 登录态")
    void updateStatus_otherUser_evictsLoginState() {
        User reader = user(2L, 1, 1, "reader01");
        when(userService.getById(2L)).thenReturn(reader);
        when(userService.updateById(reader)).thenReturn(true);

        userAdminService.updateStatus(2L, 0, 1L);

        verify(userService).updateById(reader);
        assertThat(redisCacheUtil.deletedKey).isEqualTo("login:user:2");
    }

    @Test
    @DisplayName("重置为相同密码时跳过写库与登录态清理")
    void resetPassword_sameAsOld_skips() {
        User reader = user(2L, 1, 1, "reader01");
        when(userService.getById(2L)).thenReturn(reader);

        userAdminService.resetPassword(2L, "123456");

        verify(userService, never()).updateById(any());
        assertThat(redisCacheUtil.deletedKey).isNull();
        assertThat(redisCacheUtil.changedAtKey).isNull();
    }

    @Test
    @DisplayName("重置密码后清理登录态并记录改密时间")
    void resetPassword_changed_evictsAndMarksPasswordChanged() {
        User reader = user(2L, 1, 1, "reader01");
        when(userService.getById(2L)).thenReturn(reader);
        when(userService.updateById(reader)).thenReturn(true);

        userAdminService.resetPassword(2L, "newpass123");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userService).updateById(captor.capture());
        assertThat(passwordEncoder.matches("newpass123", captor.getValue().getPassword()))
                .isTrue();
        assertThat(redisCacheUtil.deletedKey).isEqualTo("login:user:2");
        assertThat(redisCacheUtil.changedAtKey).isEqualTo("pwd:changed:2");
    }

    @Test
    @DisplayName("关联读者存在借阅记录时拒绝删除账号")
    void deleteUser_readerWithBorrowRecords_throws() {
        when(userService.getById(2L)).thenReturn(user(2L, 1, 1, "reader01"));
        when(readerService.getByUserId(2L)).thenReturn(reader(20L, 2L));
        doThrow(new BizException(ResultCode.PARAM_INVALID, "该读者存在借阅记录，不能删除"))
                .when(readerService).deleteReader(20L);

        assertThatThrownBy(() -> userAdminService.deleteUser(2L, 1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("借阅记录");

        verify(readerService).deleteReader(20L);
        verify(userService, never()).removeById(2L);
    }

    @Test
    @DisplayName("无借阅记录时删除读者档案并改写墓碑用户名")
    void deleteUser_noBorrow_renamesUsernameAndDeletesReader() {
        User readerUser = user(2L, 1, 1, "reader01");
        when(userService.getById(2L)).thenReturn(readerUser);
        when(readerService.getByUserId(2L)).thenReturn(reader(20L, 2L));
        when(userService.updateById(any(User.class))).thenReturn(true);
        when(userService.removeById(2L)).thenReturn(true);

        userAdminService.deleteUser(2L, 1L);

        verify(readerService).deleteReader(20L);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userService).updateById(captor.capture());
        assertThat(captor.getValue().getUsername())
                .startsWith("reader01__deleted_2_");
        assertThat(captor.getValue().getUsername().length()).isLessThanOrEqualTo(50);
        verify(userService).removeById(2L);
        assertThat(redisCacheUtil.deletedKey).isEqualTo("login:user:2");
    }

    private User user(Long userId, int role, int status, String username) {
        User user = new User();
        user.setUserId(userId);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("123456"));
        user.setRole(role);
        user.setStatus(status);
        return user;
    }

    private Reader reader(Long readerId, Long userId) {
        Reader reader = new Reader();
        reader.setReaderId(readerId);
        reader.setUserId(userId);
        return reader;
    }

    /**
     * 测试替身：只记录登录态相关键，不依赖 Redis。
     */
    private static class RecordingRedisCacheUtil extends RedisCacheUtil {
        private String deletedKey;
        private String changedAtKey;

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
        }
    }
}
