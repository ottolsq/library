package com.libraryweb.service;

import com.librarycommon.constant.SecurityConstants;
import com.librarycommon.security.LoginUser;
import com.libraryweb.util.RedisCacheUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.TimeUnit;

/**
 * 登录态失效服务。
 *
 * <p>所有会让已签发令牌失效的管理操作都走这里，避免各处重复拼 Redis 键：
 * <ul>
 *   <li>禁用、删除、改角色、改用户名：删除 Redis 登录态即可，下一次请求取不到身份</li>
 *   <li>改密、管理员重置密码：删除登录态并记录改密时间，旧令牌在用户重新登录后也不能复用</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class LoginStateService {

    private final RedisCacheUtil redisCacheUtil;

    /**
     * 写入登录态，TTL 与访问令牌有效期一致。
     *
     * @param loginUser 已装配角色与权限的登录用户
     */
    public void save(LoginUser loginUser) {
        if (loginUser == null || loginUser.getUserId() == null) {
            return;
        }
        redisCacheUtil.set(
                SecurityConstants.REDIS_LOGIN_USER_PREFIX + loginUser.getUserId(),
                loginUser,
                SecurityConstants.ACCESS_TOKEN_EXPIRE,
                TimeUnit.MILLISECONDS);
    }

    /**
     * 让指定用户的当前登录态立即失效。
     *
     * @param userId 用户主键ID
     */
    public void evict(Long userId) {
        if (userId == null) {
            return;
        }
        redisCacheUtil.delete(SecurityConstants.REDIS_LOGIN_USER_PREFIX + userId);
    }

    /**
     * 密码变更后让登录态失效，并记录改密时间。
     *
     * <p>仅删除 Redis 登录态还不够：用户重新登录后，攻击者手里的旧令牌仍能取到
     * 新的登录态。因此额外写入 {@code pwd:changed:{userId}}，
     * {@code JwtAuthenticationFilter} 会拒绝签发时间早于该时间的令牌。
     *
     * @param userId 用户主键ID
     */
    public void evictAfterPasswordChange(Long userId) {
        if (userId == null) {
            return;
        }
        evict(userId);
        redisCacheUtil.set(
                SecurityConstants.REDIS_PWD_CHANGED_PREFIX + userId,
                System.currentTimeMillis(),
                SecurityConstants.ACCESS_TOKEN_EXPIRE,
                TimeUnit.MILLISECONDS);
    }

    /**
     * 在数据库事务提交后再让登录态失效。
     *
     * <p>若在事务内直接删除 Redis，事务回滚时会出现「数据库未变、用户却被登出」
     * 的不一致。放在 afterCommit 可以保证只有真正落库的变更才影响登录态。
     */
    public void evictAfterCommit(Long userId) {
        runAfterCommit(() -> evict(userId));
    }

    /**
     * 密码变更在事务提交后让登录态失效，并记录改密时间。
     */
    public void evictAfterPasswordChangeAfterCommit(Long userId) {
        runAfterCommit(() -> evictAfterPasswordChange(userId));
    }

    private static void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            action.run();
                        }
                    });
            return;
        }
        // 非事务上下文（如单元测试）直接执行
        action.run();
    }
}
