package com.dianping.module.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dianping.common.BizException;
import com.dianping.common.JwtUtil;
import com.dianping.common.ResultCode;
import com.dianping.module.auth.LoginReq;
import com.dianping.module.user.User;
import com.dianping.module.user.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /**
     * 固定验证码。**只在 dianping.sms.mock-enabled=true 时生效**；
     * 该开关默认 false（生产安全），本地开发由 dev profile 打开。
     *
     * TODO 接入真实短信服务商：验证码写入 Redis，校验 5 分钟有效、一次性消费。
     */
    private static final String MOCK_SMS_CODE = "8888";

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final com.dianping.common.RateLimitService rateLimitService;

    @Value("${dianping.sms.mock-enabled:false}")
    private boolean smsMockEnabled;

    /**
     * 手机号 + 验证码登录；首次登录自动注册（昵称默认"用户+手机尾号"）。
     */
    public Map<String, Object> login(LoginReq req) {
        // 防刷：同一手机号 60 秒内最多 3 次登录尝试
        rateLimitService.check("login", req.phone(), 3, 60);
        verifyCode(req.phone(), req.code());

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

    /**
     * 校验短信验证码。
     *
     * 当前只有 mock 实现（固定 8888），未接入真实短信服务。因此：
     * - 关闭开关时**一律拒绝登录**，而不是放行 ——
     *   宁可"登录不可用"，也不能让一个固定验证码在线上放行任意账号（含管理员）。
     * - 打开开关时放行，但打 WARN 留痕，便于事后判断是否误开了开关。
     */
    private void verifyCode(String phone, String code) {
        if (!smsMockEnabled) {
            log.error("登录被拒绝：短信验证码服务未接入，phone={}", maskPhone(phone));
            throw new BizException(ResultCode.SYSTEM_ERROR, "短信验证码服务未接入，请联系管理员");
        }
        if (!MOCK_SMS_CODE.equals(code)) {
            throw new BizException(ResultCode.PARAM_ERROR, "验证码错误");
        }
        log.warn("使用固定验证码登录（misconfiguration 风险，仅限本机开发）phone={}", maskPhone(phone));
    }

    /** 日志脱敏：手机号属个人信息，不打全量 */
    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
