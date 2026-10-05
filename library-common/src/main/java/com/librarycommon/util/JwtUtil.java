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

    private JwtUtil() {
    }

    /**
     * 签发 access token
     *
     * @param userId 用户主键ID
     * @return JWT token
     */
    public static String createToken(Long userId) {
        return createToken(userId, SecurityConstants.JWT_SECRET);
    }

    /**
     * 签发 access token（可自定义密钥）
     *
     * @param userId 用户主键ID
     * @param secret 签名密钥
     * @return JWT token
     */
    public static String createToken(Long userId, String secret) {
        long nowSeconds = System.currentTimeMillis() / 1000;
        long expSeconds = nowSeconds + SecurityConstants.ACCESS_TOKEN_EXPIRE / 1000;

        Map<String, Object> payload = new HashMap<>();
        payload.put(SecurityConstants.JWT_USER_ID, userId);
        payload.put(SecurityConstants.JWT_ISSUED_AT, nowSeconds);
        payload.put(SecurityConstants.JWT_EXPIRATION, expSeconds);

        return JWTUtil.createToken(payload, secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 校验 token 是否有效（签名正确且未过期）
     *
     * @param token JWT token
     * @return true 有效
     */
    public static boolean validateToken(String token) {
        return validateToken(token, SecurityConstants.JWT_SECRET);
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
        try {
            JWTSigner signer = JWTSignerUtil.hs256(secret.getBytes(StandardCharsets.UTF_8));
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
        return parseUserId(token, SecurityConstants.JWT_SECRET);
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
        return getExpireSeconds(token, SecurityConstants.JWT_SECRET);
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
