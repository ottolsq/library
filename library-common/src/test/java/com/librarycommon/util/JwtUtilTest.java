package com.librarycommon.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JWT 工具测试：签发时间解析与无效令牌处理。
 */
class JwtUtilTest {

    @Test
    @DisplayName("签发的令牌可解析出签发时间")
    void createToken_canReadIssuedAt() {
        long before = System.currentTimeMillis() / 1000;
        String token = JwtUtil.createToken(1L);

        Long issuedAt = JwtUtil.getIssuedAtSeconds(token);

        assertThat(issuedAt).isNotNull();
        assertThat(issuedAt).isBetween(before, before + 2);
    }

    @Test
    @DisplayName("签发的令牌可解析出毫秒级签发时间")
    void createToken_canReadIssuedAtMillis() {
        long before = System.currentTimeMillis();
        String token = JwtUtil.createToken(1L);

        Long issuedAt = JwtUtil.getIssuedAtMillis(token);

        assertThat(issuedAt).isNotNull();
        assertThat(issuedAt).isBetween(before, before + 2_000L);
    }

    @Test
    @DisplayName("无效令牌返回 null")
    void invalidToken_returnsNull() {
        assertThat(JwtUtil.getIssuedAtSeconds("not-a-jwt")).isNull();
        assertThat(JwtUtil.getIssuedAtMillis("not-a-jwt")).isNull();
    }
}
