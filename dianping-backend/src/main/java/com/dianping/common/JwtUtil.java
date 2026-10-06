package com.dianping.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：subject 存 userId，claim 存 role。
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireMillis;

    public JwtUtil(@Value("${dianping.jwt.secret:}") String secret,
                   @Value("${dianping.jwt.expire-days}") int expireDays) {
        // 启动即校验密钥：不给公开的默认值，缺了就起不来。
        // 原因：仓库是公开的，配置里任何默认密钥都等于公开密钥，
        // 攻击者可以直接签发任意用户（含管理员）的令牌绕过登录。
        // 宁可启动失败，也不能默认不安全。
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "缺少 JWT 密钥。请通过环境变量 JWT_SECRET 提供（至少 32 字节）；"
                    + "本地开发请用 --spring.profiles.active=dev 启动。");
        }
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        // HS256 的密钥长度是硬性要求，配短了 jjwt 会抛，提前给出可读提示
        if (raw.length < 32) {
            throw new IllegalStateException(
                    "JWT 密钥过短：HS256 要求至少 32 字节，当前 " + raw.length + " 字节。");
        }
        this.key = Keys.hmacShaKeyFor(raw);
        this.expireMillis = expireDays * 24L * 3600 * 1000;
    }

    public String create(Long userId, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMillis))
                .signWith(key)
                .compact();
    }

    /** 校验失败会抛 JwtException / IllegalArgumentException，由拦截器统一转成 1002 */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
    }
}
