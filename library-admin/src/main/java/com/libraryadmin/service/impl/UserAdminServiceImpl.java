package com.libraryadmin.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.libraryadmin.dto.CreateUserDTO;
import com.libraryadmin.dto.UpdateUserDTO;
import com.libraryadmin.entity.User;
import com.libraryadmin.service.UserAdminService;
import com.libraryadmin.service.UserService;
import com.librarycommon.exception.BizException;
import com.librarycommon.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 系统用户管理服务实现
 *
 * <p>仅做参数与业务规则的最小校验，权限校验由方法注解 {@code @PreAuthorize} 统一拦截。
 */
@Service
@RequiredArgsConstructor
public class UserAdminServiceImpl implements UserAdminService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public IPage<User> pageUsers(long pageNum, long pageSize) {
        return userService.page(new Page<>(pageNum, pageSize));
    }

    @Override
    public User getById(Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "用户不存在");
        }
        return user;
    }

    @Override
    @Transactional
    public Long createUser(CreateUserDTO dto) {
        // 用户名唯一性校验
        if (userService.getByUsername(dto.username()) != null) {
            throw new BizException(ResultCode.DATA_ALREADY_EXISTS, "账号已存在");
        }
        User user = new User();
        user.setUsername(dto.username());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setRole(dto.role());
        user.setStatus(dto.status().getCode());
        userService.save(user);
        return user.getUserId();
    }

    @Override
    @Transactional
    public void updateUser(Long userId, UpdateUserDTO dto) {
        User user = getById(userId);
        if (dto.username() != null && !dto.username().equals(user.getUsername())) {
            if (userService.getByUsername(dto.username()) != null) {
                throw new BizException(ResultCode.DATA_ALREADY_EXISTS, "账号已存在");
            }
            user.setUsername(dto.username());
        }
        if (dto.role() != null) {
            user.setRole(dto.role());
        }
        if (dto.status() != null) {
            user.setStatus(dto.status().getCode());
        }
        userService.updateById(user);
    }

    @Override
    @Transactional
    public void updateStatus(Long userId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ResultCode.PARAM_INVALID, "状态值非法");
        }
        boolean ok = userService.update(
                new LambdaUpdateWrapper<User>()
                        .eq(User::getUserId, userId)
                        .set(User::getStatus, status));
        if (!ok) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "用户不存在");
        }
    }

    @Override
    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new BizException(ResultCode.PARAM_INVALID, "新密码不能为空");
        }
        User user = userService.getById(userId);
        if (user == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "用户不存在");
        }
        // 与历史密码相同则跳过，避免无意义的写
        if (Objects.equals(user.getPassword(), passwordEncoder.encode(newPassword))) {
            return;
        }
        boolean ok = userService.update(
                new LambdaUpdateWrapper<User>()
                        .eq(User::getUserId, userId)
                        .set(User::getPassword, passwordEncoder.encode(newPassword)));
        if (!ok) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "用户不存在");
        }
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        // MyBatis-Plus 已配置逻辑删除字段，removeById 会转为 update is_deleted=1
        boolean ok = userService.removeById(userId);
        if (!ok) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "用户不存在");
        }
    }
}