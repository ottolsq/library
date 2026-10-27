package com.librarycommon.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;
import com.librarycommon.constant.SecurityConstants;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 *
 * <p>对外只提供签发、校验、解析用户 ID、读取有效期四项能力。
 * <p>载荷仅保留：用户ID、签发时间、过期时间，不存放任何敏感信息。
 */
public final class JwtUtil {

    /**
     * 当前签名密钥：应用启动时从 {@code security.jwt.secret} 注入
     * （环境变量 {@code JWT_SECRET}）。未配置时相关方法会抛出异常，
     * 避免使用硬编码密钥。
     */
    private static volatile String secret;

    private JwtUtil() {
    }

    /**
     * 设置签名密钥（由 JwtSecretConfig 在启动时调用）
     */
    public static void setSecret(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("JWT 签名密钥不能为空");
        }
        secret = value;
    }

    /**
     * 读取当前签名密钥（仅供测试与诊断使用）
     */
    public static String getSecret() {
        return secret;
    }

    /**
     * 取出已配置的签名密钥；未配置时直接失败，避免使用硬编码或空密钥。
     */
    private static String requireSecret() {
        String current = secret;
        if (current == null || current.isBlank()) {
            throw new IllegalStateException(
                    "JWT 签名密钥未配置，请设置 JWT_SECRET 环境变量");
        }
        return current;
    }

    private static String requireSecret(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("JWT 签名密钥不能为空");
        }
        return value;
    }

    /**
     * 签发 access token
     *
     * @param userId 用户主键ID
     * @return JWT token
     */
    public static String createToken(Long userId) {
        return createToken(userId, requireSecret());
    }

    /**
     * 签发 access token（可自定义密钥）
     *
     * @param userId 用户主键ID
     * @param secret 签名密钥
     * @return JWT token
     */
    public static String createToken(Long userId, String secret) {
        String signKey = requireSecret(secret);
        long nowSeconds = System.currentTimeMillis() / 1000;
        long expSeconds = nowSeconds + SecurityConstants.ACCESS_TOKEN_EXPIRE / 1000;

        Map<String, Object> payload = new HashMap<>();
        payload.put(SecurityConstants.JWT_USER_ID, userId);
        payload.put(SecurityConstants.JWT_ISSUED_AT, nowSeconds);
        // 毫秒级签发时间：改密后用于精确拒绝同一秒内签发的旧令牌
        payload.put(SecurityConstants.JWT_ISSUED_AT_MS, System.currentTimeMillis());
        payload.put(SecurityConstants.JWT_EXPIRATION, expSeconds);

        return JWTUtil.createToken(payload, signKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 校验 token 是否有效（签名正确且未过期）
     *
     * @param token JWT token
     * @return true 有效
     */
    public static boolean validateToken(String token) {
        return validateToken(token, requireSecret());
    }

    /**
     * 校验 token 是否有效（可自定义密钥）
     *
     * @param token  JWT token
     * @param secret 签名密钥
     * @return true 有效
     */
    public static boolean validateToken(String token, String secret) {
        if (StrUtil.isBlank(token)) {
            return false;
        }
        String signKey = requireSecret(secret);
        try {
            JWTSigner signer = JWTSignerUtil.hs256(signKey.getBytes(StandardCharsets.UTF_8));
            // Hutool verify 会同时校验签名与 exp 是否过期
            return JWTUtil.verify(token, signer);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从 token 中解析用户 ID（会先校验 token 有效性）
     *
     * @param token JWT token
     * @return 用户主键ID，无效 token 返回 null
     */
    public static Long parseUserId(String token) {
        return parseUserId(token, requireSecret());
    }

    /**
     * 从 token 中解析用户 ID（可自定义密钥）
     *
     * @param token  JWT token
     * @param secret 签名密钥
     * @return 用户主键ID，无效 token 返回 null
     */
    public static Long parseUserId(String token, String secret) {
        if (!validateToken(token, secret)) {
            return null;
        }
        try {
            return Convert.toLong(JWTUtil.parseToken(token).getPayload(SecurityConstants.JWT_USER_ID));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 读取 token 有效期（秒）
     *
     * @param token JWT token
     * @return 有效期秒数，无效 token 返回 0
     */
    public static long getExpireSeconds(String token) {
        return getExpireSeconds(token, requireSecret());
    }

    /**
     * 读取 token 有效期（秒，可自定义密钥）
     *
     * <p>从 token 的 exp 载荷计算剩余秒数；签发后第一次读取为完整 24 小时，
     * 随着时间的推移逐渐减少，token 过期后返回 0。
     *
     * @param token  JWT token
     * @param secret 签名密钥
     * @return 有效期秒数，无效 token 返回 0
     */
    public static long getExpireSeconds(String token, String secret) {
        if (!validateToken(token, secret)) {
            return 0;
        }
        try {
            Long exp = Convert.toLong(JWTUtil.parseToken(token).getPayload(SecurityConstants.JWT_EXPIRATION));
            if (exp == null) {
                return 0;
            }
            long remain = exp - System.currentTimeMillis() / 1000;
            return Math.max(remain, 0);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 读取 token 的签发时间（秒）
     *
     * <p>用于在改密后判断令牌是否早于改密时间，从而拒绝所有旧令牌。
     *
     * @param token JWT token
     * @return 签发时间（epoch 秒），无效 token 返回 null
     */
    public static Long getIssuedAtSeconds(String token) {
        return getIssuedAtSeconds(token, requireSecret());
    }

    /**
     * 读取 token 的签发时间（秒，可自定义密钥）
     */
    public static Long getIssuedAtSeconds(String token, String secret) {
        if (!validateToken(token, secret)) {
            return null;
        }
        try {
            return Convert.toLong(
                    JWTUtil.parseToken(token).getPayload(SecurityConstants.JWT_ISSUED_AT));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 读取 token 的签发时间（毫秒）
     *
     * <p>用于改密后精确判定：旧令牌的签发时间早于改密时间即拒绝。
     *
     * @param token JWT token
     * @return 签发时间（epoch 毫秒），无效 token 或无该字段时返回 null
     */
    public static Long getIssuedAtMillis(String token) {
        return getIssuedAtMillis(token, requireSecret());
    }

    /**
     * 读取 token 的签发时间（毫秒，可自定义密钥）
     */
    public static Long getIssuedAtMillis(String token, String secret) {
        if (!validateToken(token, secret)) {
            return null;
        }
        try {
            return Convert.toLong(JWTUtil.parseToken(token)
                    .getPayload(SecurityConstants.JWT_ISSUED_AT_MS));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 去掉 token 前缀
     *
     * @param tokenWithPrefix 可能带有 Bearer 前缀的 token
     * @return 去掉前缀后的 token
     */
    public static String stripTokenPrefix(String tokenWithPrefix) {
        if (tokenWithPrefix == null || tokenWithPrefix.isBlank()) {
            return "";
        }
        String prefix = SecurityConstants.TOKEN_PREFIX;
        if (tokenWithPrefix.startsWith(prefix)) {
            return tokenWithPrefix.substring(prefix.length()).trim();
        }
        return tokenWithPrefix;
    }
}
