package com.libraryadmin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.libraryadmin.dto.CreateUserDTO;
import com.libraryadmin.dto.ResetPasswordDTO;
import com.libraryadmin.dto.UpdateUserDTO;
import com.libraryadmin.dto.UserDetailVO;
import com.libraryadmin.dto.UserListItemVO;
import com.libraryadmin.entity.User;
import com.libraryadmin.service.UserAdminService;
import com.librarycommon.enums.UserStatus;
import com.librarycommon.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 系统用户管理接口
 *
 * <p>所有方法均挂 {@code @PreAuthorize("@ss.hasPermission('user:xxx')")}，
 * 与 {@code SecurityConstants.PERMISSION_RULES} 协同实现路径级 + 方法级双重校验。
 * <p>{@code @EnableMethodSecurity} 已在 {@code SecurityConfig} 开启，注解才会生效。
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService userAdminService;

    /**
     * 分页查询用户列表
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermission('user:list')")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        IPage<User> page = userAdminService.pageUsers(pageNum, pageSize);
        List<UserListItemVO> items = page.getRecords().stream()
                .map(u -> new UserListItemVO(
                        u.getUserId(),
                        u.getUsername(),
                        u.getRole(),
                        u.getStatus() == null ? null : UserStatus.from(u.getStatus())))
                .toList();
        return Result.ok(Map.of(
                "total", page.getTotal(),
                "pageNum", page.getCurrent(),
                "pageSize", page.getSize(),
                "items", items
        ));
    }

    /**
     * 查询用户详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermission('user:query')")
    public Result<UserDetailVO> detail(@PathVariable Long id) {
        User u = userAdminService.getById(id);
        return Result.ok(new UserDetailVO(
                u.getUserId(),
                u.getUsername(),
                u.getRole(),
                u.getStatus() == null ? null : UserStatus.from(u.getStatus()),
                u.getCreateTime()
        ));
    }

    /**
     * 新增用户
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermission('user:add')")
    public Result<Long> create(@Valid @RequestBody CreateUserDTO dto) {
        Long id = userAdminService.createUser(dto);
        return Result.ok(id);
    }

    /**
     * 修改用户资料
     */
    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermission('user:edit')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateUserDTO dto) {
        userAdminService.updateUser(id, dto);
        return Result.ok();
    }

    /**
     * 启用或禁用账号
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasPermission('user:status')")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userAdminService.updateStatus(id, status);
        return Result.ok();
    }

    /**
     * 重置密码
     */
    @PutMapping("/{id}/password")
    @PreAuthorize("@ss.hasPermission('user:resetPwd')")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordDTO dto) {
        userAdminService.resetPassword(id, dto.newPassword());
        return Result.ok();
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermission('user:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        userAdminService.deleteUser(id);
        return Result.ok();
    }
}