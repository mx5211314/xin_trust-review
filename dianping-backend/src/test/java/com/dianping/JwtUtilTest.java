package com.dianping;

import com.dianping.common.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** JWT 签发/解析/防篡改 */
class JwtUtilTest {

    private final JwtUtil jwtUtil = new JwtUtil("unit-test-secret-key-0123456789-abcdefghijk", 7);

    @Test
    @DisplayName("签发后解析：subject=userId, role 正确")
    void createAndParse() {
        String token = jwtUtil.create(12345L, "REVIEWER");
        Claims claims = jwtUtil.parse(token);
        assertEquals("12345", claims.getSubject());
        assertEquals("REVIEWER", claims.get("role", String.class));
    }

    @Test
    @DisplayName("被篡改的 token 解析必须失败")
    void tamperedTokenRejected() {
        String token = jwtUtil.create(1L, "USER");
        String tampered = token.substring(0, token.length() - 3) + "abc";
        assertThrows(Exception.class, () -> jwtUtil.parse(tampered));
    }

    @Test
    @DisplayName("换密钥签发的 token 不被接受")
    void otherSecretRejected() {
        JwtUtil other = new JwtUtil("another-secret-key-0123456789-abcdefghijklmn", 7);
        String token = other.create(1L, "ADMIN");
        assertThrows(Exception.class, () -> jwtUtil.parse(token));
    }
}
