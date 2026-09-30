package com.dianping.common;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录 + 角色拦截器：
 * 1. 解析 Authorization: Bearer <token>，失败 -> 1002
 * 2. 方法上有 @RequireRole 时校验角色；ADMIN 拥有全部角色权限 -> 1003
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
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
        String role = claims.get("role", String.class);
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

    private boolean hasRole(String role, String[] allowed) {
        if ("ADMIN".equals(role)) {
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
