package com.dianping.common;

import com.dianping.module.user.User;
import com.dianping.module.user.UserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * 登录 + 角色拦截器：
 * 1. 解析 Authorization: Bearer <token>，失败 -> 1002
 * 2. 【安全】以数据库/缓存中的用户状态与角色为准（Redis 缓存 60s）——
 *    token 里的 role 只作快速路径，封禁/授权/收权在 60 秒内生效（管理员操作时主动清缓存）
 * 3. 方法上有 @RequireRole 时校验角色；ADMIN 拥有全部角色权限 -> 1003
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String NOT_EXIST = "-";

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final StringRedisTemplate redis;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 先清理：拦截器抛异常时 afterCompletion 不会执行，避免线程池脏数据
        UserContext.clear();
        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        Claims claims;
        try {
            claims = jwtUtil.parse(auth.substring(7));
        } catch (Exception e) {
            throw new BizException(ResultCode.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        long userId = Long.parseLong(claims.getSubject());

        // 以库中状态为准（Redis 缓存），保证封禁/角色变更即时生效
        String[] u = loadUser(userId);
        String status = u[0];
        String role = u[1];
        if (status == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "账号不存在，请重新登录");
        }
        if (DianpingConst.USER_BANNED.equals(status)) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已被封禁，请联系管理员");
        }
        UserContext.set(userId, role);

        RequireRole rr = hm.getMethodAnnotation(RequireRole.class);
        if (rr != null && !hasRole(role, rr.value())) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }

    /** 读取用户状态与角色：[status, role]，不存在时 [null, null]；Redis 缓存 60s */
    private String[] loadUser(long userId) {
        String key = DianpingConst.REDIS_USER_STATUS + userId;
        String cached = null;
        try {
            cached = redis.opsForValue().get(key);
        } catch (Exception ignored) {
            // Redis 不可用：走 DB
        }
        if (cached == null) {
            User u = userMapper.selectById(userId);
            cached = u == null ? NOT_EXIST : (u.getStatus() + "|" + u.getRole());
            try {
                redis.opsForValue().set(key, cached, Duration.ofSeconds(DianpingConst.USER_STATUS_TTL_SECONDS));
            } catch (Exception ignored) {
            }
        }
        if (NOT_EXIST.equals(cached)) {
            return new String[]{null, null};
        }
        String[] parts = cached.split("\\|", 2);
        return new String[]{parts[0], parts.length > 1 ? parts[1] : null};
    }

    private boolean hasRole(String role, String[] allowed) {
        if (DianpingConst.ROLE_ADMIN.equals(role)) {
            return true;
        }
        for (String r : allowed) {
            if (r.equals(role)) {
                return true;
            }
        }
        return false;
    }
}
