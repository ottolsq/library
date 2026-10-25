package com.libraryadmin.service;

import com.libraryadmin.dto.RegisterDTO;
import com.libraryadmin.entity.Reader;
import com.libraryadmin.entity.User;
import com.librarycommon.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 读者自助注册业务测试。
 */
@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private ReaderService readerService;

    private PasswordEncoder passwordEncoder;
    private RegisterService registerService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        registerService = new RegisterService(userService, readerService, passwordEncoder);
    }

    @Test
    @DisplayName("注册成功时创建读者账号与读者档案")
    void register_success_createsUserAndReader() {
        when(userService.getByUsername("newreader")).thenReturn(null);
        when(readerService.getByStudentId("2024999999")).thenReturn(null);
        when(userService.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(99L);
            return true;
        });
        when(readerService.save(any(Reader.class))).thenReturn(true);

        Long userId = registerService.register(validDto());

        assertThat(userId).isEqualTo(99L);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("newreader");
        assertThat(savedUser.getRole()).isEqualTo(1);
        assertThat(savedUser.getStatus()).isEqualTo(1);
        assertThat(passwordEncoder.matches("123456", savedUser.getPassword())).isTrue();

        ArgumentCaptor<Reader> readerCaptor = ArgumentCaptor.forClass(Reader.class);
        verify(readerService).save(readerCaptor.capture());
        Reader savedReader = readerCaptor.getValue();
        assertThat(savedReader.getUserId()).isEqualTo(99L);
        assertThat(savedReader.getReaderName()).isEqualTo("新读者");
        assertThat(savedReader.getStudentId()).isEqualTo("2024999999");
        assertThat(savedReader.getPhone()).isEqualTo("13800000009");
        assertThat(savedReader.getMaxBorrowCnt()).isEqualTo(5);
        assertThat(savedReader.getStatus()).isEqualTo(1);
    }

    @Test
    @DisplayName("账号已存在时拒绝注册且不写库")
    void register_duplicateUsername_throws() {
        when(userService.getByUsername("newreader")).thenReturn(new User());

        assertThatThrownBy(() -> registerService.register(validDto()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("账号已存在");

        verify(userService, never()).save(any());
        verify(readerService, never()).save(any());
    }

    @Test
    @DisplayName("学号已存在时拒绝注册且不写库")
    void register_duplicateStudentId_throws() {
        when(userService.getByUsername("newreader")).thenReturn(null);
        when(readerService.getByStudentId("2024999999")).thenReturn(new Reader());

        assertThatThrownBy(() -> registerService.register(validDto()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("学号已存在");

        verify(userService, never()).save(any());
        verify(readerService, never()).save(any());
    }

    @Test
    @DisplayName("两次密码不一致时拒绝注册")
    void register_passwordMismatch_throws() {
        RegisterDTO dto = new RegisterDTO(
                "newreader", "123456", "654321",
                "新读者", "2024999999", "13800000009");

        assertThatThrownBy(() -> registerService.register(dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("两次输入的密码不一致");
    }

    private RegisterDTO validDto() {
        return new RegisterDTO(
                "newreader", "123456", "123456",
                "新读者", "2024999999", "13800000009");
    }
}
