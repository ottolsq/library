package com.libraryadmin.service;

import com.libraryadmin.dto.RegisterDTO;
import com.libraryadmin.entity.Reader;
import com.libraryadmin.entity.User;
import com.librarycommon.exception.BizException;
import com.librarycommon.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 读者自助注册业务。
 *
 * <p>一次注册同时创建系统账号与读者档案：
 * 账号角色固定为读者（1），状态正常；读者默认最多可借 5 本。
 * 任一步失败由事务整体回滚，避免出现“有账号无档案”的半成品数据。
 */
@Service
@RequiredArgsConstructor
public class RegisterService {

    private static final int READER_ROLE = 1;
    private static final int ENABLED = 1;
    private static final int DEFAULT_MAX_BORROW = 5;

    private final UserService userService;
    private final ReaderService readerService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long register(RegisterDTO dto) {
        if (!dto.password().equals(dto.confirmPassword())) {
            throw new BizException(ResultCode.PARAM_INVALID, "两次输入的密码不一致");
        }
        if (userService.getByUsername(dto.username()) != null) {
            throw new BizException(ResultCode.DATA_ALREADY_EXISTS, "账号已存在");
        }
        if (readerService.getByStudentId(dto.studentId()) != null) {
            throw new BizException(ResultCode.DATA_ALREADY_EXISTS, "学号已存在");
        }

        // 角色固定为读者，不接受前端传入的角色值
        User user = new User();
        user.setUsername(dto.username());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setRole(READER_ROLE);
        user.setStatus(ENABLED);
        userService.save(user);

        Reader reader = new Reader();
        reader.setUserId(user.getUserId());
        reader.setReaderName(dto.readerName());
        reader.setStudentId(dto.studentId());
        reader.setPhone(dto.phone());
        reader.setMaxBorrowCnt(DEFAULT_MAX_BORROW);
        reader.setStatus(ENABLED);
        readerService.save(reader);

        return user.getUserId();
    }
}
