package com.libraryweb.config;

import com.librarycommon.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT 密钥配置
 *
 * <p>启动时把 {@code security.jwt.secret} 注入 {@link JwtUtil}，
 * 避免把签名密钥硬编码在代码中。生产环境通过环境变量 {@code JWT_SECRET} 提供。
 */
@Component
public class JwtSecretConfig {

    public JwtSecretConfig(@Value("${security.jwt.secret}") String secret) {
        JwtUtil.setSecret(secret);
    }
}
