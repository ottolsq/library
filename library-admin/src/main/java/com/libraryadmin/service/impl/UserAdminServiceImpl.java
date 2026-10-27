package com.libraryadmin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.libraryadmin.dto.CreateUserDTO;
import com.libraryadmin.dto.UpdateUserDTO;
import com.libraryadmin.entity.Reader;
import com.libraryadmin.entity.User;
import com.libraryadmin.service.ReaderService;
import com.libraryadmin.service.UserAdminService;
import com.libraryadmin.service.UserService;
import com.librarycommon.enums.UserRole;
import com.librarycommon.enums.UserStatus;
import com.librarycommon.exception.BizException;
import com.librarycommon.result.ResultCode;
import com.libraryweb.service.LoginStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 系统用户管理服务实现
 *
 * <p>除参数与业务规则校验外，还负责两件事：
 * <ul>
 *   <li>自我保护：禁止禁用、删除、降级当前登录账号，禁止操作最后一个启用的管理员</li>
 *   <li>登录态同步：禁用、删除、改角色、改用户名、重置密码后清理 Redis 登录态，
 *       避免旧令牌继续访问</li>
 * </ul>
 *
 * <p>登录态清理注册在事务提交之后执行，避免数据库回滚却把用户登出。
 */
@Service
@RequiredArgsConstructor
public class UserAdminServiceImpl implements UserAdminService {

    private static final int USERNAME_MAX_LENGTH = 50;

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final LoginStateService loginStateService;
    private final ReaderService readerService;

    @Override
    public IPage<User> pageUsers(long pageNum, long pageSize, Integer role, Boolean unlinked) {
        if (role != null) {
            validateRole(role);
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(role != null, User::getRole, role);
        // 新增读者时只列出尚未关联读者档案的账号，避免下拉出现重复项
        wrapper.notInSql(Boolean.TRUE.equals(unlinked), User::getUserId,
                "SELECT user_id FROM reader");
        wrapper.orderByDesc(User::getUserId);
        return userService.page(new Page<>(pageNum, pageSize), wrapper);
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
        validateRole(dto.role());
        validatePassword(dto.password());
        String username = normalizeUsername(dto.username());
        // 用户名唯一性校验
        if (userService.getByUsername(username) != null) {
            throw new BizException(ResultCode.DATA_ALREADY_EXISTS, "账号已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setRole(dto.role());
        user.setStatus(dto.status().getCode());
        try {
            if (!userService.save(user)) {
                throw new BizException(ResultCode.SYSTEM_ERROR, "新增用户失败");
            }
        } catch (DuplicateKeyException e) {
            throw new BizException(ResultCode.DATA_ALREADY_EXISTS,
                    "账号已存在（可能被已删除账号占用）");
        }
        return user.getUserId();
    }

    @Override
    @Transactional
    public void updateUser(Long userId, UpdateUserDTO dto, Long currentUserId) {
        User user = getById(userId);
        // 用修改前的角色/状态做最后管理员判断，避免“先升管理员再禁用”被误判
        boolean wasAdmin = isAdmin(user);
        boolean wasEnabled = isEnabled(user);
        boolean securityChanged = false;

        if (dto.username() != null) {
            String username = normalizeUsername(dto.username());
            if (!username.equals(user.getUsername())) {
                if (Objects.equals(userId, currentUserId)) {
                    throw new BizException(ResultCode.FORBIDDEN, "不能修改当前登录账号的账号名");
                }
                User existing = userService.getByUsername(username);
                // 大小写变更时 MySQL 不区分大小写会命中自己，需排除自身
                if (existing != null && !Objects.equals(existing.getUserId(), userId)) {
                    throw new BizException(ResultCode.DATA_ALREADY_EXISTS, "账号已存在");
                }
                user.setUsername(username);
                securityChanged = true;
            }
        }

        if (dto.role() != null) {
            validateRole(dto.role());
            if (!Objects.equals(dto.role(), user.getRole())) {
                if (Objects.equals(userId, currentUserId)) {
                    throw new BizException(ResultCode.FORBIDDEN, "不能修改当前登录账号的角色");
                }
                if (wasAdmin && !Objects.equals(dto.role(), UserRole.ADMIN.getCode())) {
                    ensureNotLastEnabledAdmin(wasAdmin, wasEnabled, "降级");
                }
                user.setRole(dto.role());
                securityChanged = true;
            }
        }

        if (dto.status() != null) {
            Integer nextStatus = dto.status().getCode();
            if (!Objects.equals(nextStatus, user.getStatus())) {
                if (UserStatus.DISABLED.getCode().equals(nextStatus)
                        && Objects.equals(userId, currentUserId)) {
                    throw new BizException(ResultCode.FORBIDDEN, "不能禁用当前登录账号");
                }
                if (UserStatus.DISABLED.getCode().equals(nextStatus) && wasAdmin) {
                    ensureNotLastEnabledAdmin(wasAdmin, wasEnabled, "禁用");
                }
                user.setStatus(nextStatus);
                securityChanged = true;
            }
        }

        if (securityChanged) {
            try {
                if (!userService.updateById(user)) {
                    throw new BizException(ResultCode.DATA_NOT_FOUND, "用户不存在或已被删除");
                }
            } catch (DuplicateKeyException e) {
                throw new BizException(ResultCode.DATA_ALREADY_EXISTS,
                        "账号已存在（可能被已删除账号占用）");
            }
            // 用户名、角色或状态变化后，Redis 中的旧档案不再可信，提交后立即失效
            loginStateService.evictAfterCommit(userId);
        }
    }

    @Override
    @Transactional
    public void updateStatus(Long userId, Integer status, Long currentUserId) {
        if (!UserStatus.DISABLED.getCode().equals(status)
                && !UserStatus.ENABLED.getCode().equals(status)) {
            throw new BizException(ResultCode.PARAM_INVALID, "状态值非法");
        }
        User user = getById(userId);
        if (Objects.equals(status, user.getStatus())) {
            return;
        }
        boolean admin = isAdmin(user);
        boolean enabled = isEnabled(user);
        if (UserStatus.DISABLED.getCode().equals(status)
                && Objects.equals(userId, currentUserId)) {
            throw new BizException(ResultCode.FORBIDDEN, "不能禁用当前登录账号");
        }
        if (UserStatus.DISABLED.getCode().equals(status) && admin) {
            ensureNotLastEnabledAdmin(admin, enabled, "禁用");
        }
        user.setStatus(status);
        if (!userService.updateById(user)) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "用户不存在或已被删除");
        }
        // 状态变化后旧登录态不再可信：禁用后必须立即退出，启用后也要求重新登录
        loginStateService.evictAfterCommit(userId);
    }

    @Override
    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        validatePassword(newPassword);
        User user = getById(userId);
        // BCrypt 哈希每次加盐不同，必须用 matches 比较明文，不能比较哈希
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            return;
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        if (!userService.updateById(user)) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "用户不存在或已被删除");
        }
        loginStateService.evictAfterPasswordChangeAfterCommit(userId);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId, Long currentUserId) {
        User user = getById(userId);
        if (Objects.equals(userId, currentUserId)) {
            throw new BizException(ResultCode.FORBIDDEN, "不能删除当前登录账号");
        }
        ensureNotLastEnabledAdmin(isAdmin(user), isEnabled(user), "删除");

        // 读者档案删除规则统一由 ReaderService 维护（有关联借阅记录时会拒绝）
        Reader reader = readerService.getByUserId(userId);
        if (reader != null) {
            readerService.deleteReader(reader.getReaderId());
        }

        // 逻辑删除前把用户名改成唯一墓碑名，释放用户名唯一索引，
        // 否则同名账号永远无法重新注册
        user.setUsername(deletedUsername(user));
        if (!userService.updateById(user)) {
            throw new BizException(ResultCode.SYSTEM_ERROR, "删除账号失败：用户名改写失败");
        }
        if (!userService.removeById(userId)) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "用户不存在或已被删除");
        }
        loginStateService.evictAfterCommit(userId);
    }

    /**
     * 校验角色取值：0 管理员 / 1 读者。
     */
    private void validateRole(Integer role) {
        if (!UserRole.ADMIN.getCode().equals(role)
                && !UserRole.READER.getCode().equals(role)) {
            throw new BizException(ResultCode.PARAM_INVALID, "角色值非法");
        }
    }

    /**
     * 规范化用户名：去除首尾空白并校验非空与长度。
     *
     * <p>避免把只含空白的账号写入数据库，导致账号无法登录。
     */
    private String normalizeUsername(String username) {
        String trimmed = username == null ? "" : username.trim();
        if (trimmed.isBlank()) {
            throw new BizException(ResultCode.PARAM_INVALID, "账号不能为空");
        }
        if (trimmed.length() < 3 || trimmed.length() > 32) {
            throw new BizException(ResultCode.PARAM_INVALID, "账号长度应为 3-32 位");
        }
        return trimmed;
    }

    /**
     * 校验密码长度，避免绕过接口层校验直接调用 Service。
     */
    private void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new BizException(ResultCode.PARAM_INVALID, "新密码不能为空");
        }
        if (password.length() < 6 || password.length() > 64) {
            throw new BizException(ResultCode.PARAM_INVALID, "密码长度应为 6-64 位");
        }
    }

    /**
     * 禁止禁用、删除、降级最后一个启用的管理员。
     *
     * <p>使用 {@code FOR UPDATE} 锁定当前所有启用管理员行，
     * 防止两个管理员并发互相禁用/降级后系统不再有可用管理员。
     *
     * @param admin   操作前的角色是否为管理员
     * @param enabled 操作前的状态是否启用
     */
    private void ensureNotLastEnabledAdmin(boolean admin, boolean enabled, String action) {
        if (!admin || !enabled) {
            return;
        }
        List<User> activeAdmins = userService.list(new LambdaQueryWrapper<User>()
                .eq(User::getRole, UserRole.ADMIN.getCode())
                .eq(User::getStatus, UserStatus.ENABLED.getCode())
                .orderByAsc(User::getUserId)
                .last("FOR UPDATE"));
        if (activeAdmins == null || activeAdmins.size() <= 1) {
            throw new BizException(ResultCode.PARAM_INVALID,
                    "不能" + action + "最后一个启用的管理员账号");
        }
    }

    private boolean isAdmin(User user) {
        return UserRole.ADMIN.getCode().equals(user.getRole());
    }

    private boolean isEnabled(User user) {
        return UserStatus.ENABLED.getCode().equals(user.getStatus());
    }

    /**
     * 逻辑删除时使用的唯一墓碑用户名，长度不超过列宽 50。
     */
    private String deletedUsername(User user) {
        String suffix = "__deleted_" + user.getUserId() + "_"
                + UUID.randomUUID().toString().substring(0, 8);
        String base = user.getUsername() == null ? "" : user.getUsername();
        int maxBaseLength = Math.max(USERNAME_MAX_LENGTH - suffix.length(), 0);
        if (base.length() > maxBaseLength) {
            base = base.substring(0, maxBaseLength);
        }
        return base + suffix;
    }
}
