package com.dianping.module.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dianping.common.BizException;
import com.dianping.common.JwtUtil;
import com.dianping.common.ResultCode;
import com.dianping.module.auth.LoginReq;
import com.dianping.module.user.User;
import com.dianping.module.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    /** TODO 接短信服务商后，验证码写入 Redis 并校验 5 分钟有效 */
    private static final String DEV_SMS_CODE = "8888";

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final com.dianping.common.RateLimitService rateLimitService;

    /**
     * 手机号 + 验证码登录；首次登录自动注册（昵称默认"用户+手机尾号"）。
     */
    public Map<String, Object> login(LoginReq req) {
        // 防刷：同一手机号 60 秒内最多 3 次登录尝试
        rateLimitService.check("login", req.phone(), 3, 60);
        if (!DEV_SMS_CODE.equals(req.code())) {
            throw new BizException(ResultCode.PARAM_ERROR, "验证码错误");
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, req.phone()));
        boolean isNew = false;
        if (user == null) {
            user = new User();
            user.setPhone(req.phone());
            user.setNickname("用户" + req.phone().substring(7));
            user.setRole("USER");
            user.setStatus("NORMAL");
            userMapper.insert(user);
            isNew = true;
        }
        if ("BANNED".equals(user.getStatus())) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已被封禁，请联系管理员");
        }
        String token = jwtUtil.create(user.getId(), user.getRole());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", String.valueOf(user.getId()));
        data.put("role", user.getRole());
        data.put("isNew", isNew);
        return data;
    }
}
