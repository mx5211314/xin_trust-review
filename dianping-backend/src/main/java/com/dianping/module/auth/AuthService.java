package com.dianping.module.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dianping.common.BizException;
import com.dianping.common.JwtUtil;
import com.dianping.common.ResultCode;
import com.dianping.module.user.User;
import com.dianping.module.user.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final com.dianping.common.RateLimitService rateLimitService;
    private final SmsCodeService smsCodeService;

    /** 同一手机号每小时最多下发几次验证码（防持续骚扰与短信费用攻击） */
    @org.springframework.beans.factory.annotation.Value("${dianping.sms.hourly-send-limit:5}")
    private int sendHourlyLimit;

    /**
     * 手机号 + 验证码登录；首次登录自动注册（昵称默认"用户+手机尾号"）。
     *
     * 验证码的生成、有效期、一次性消费、错误次数、发送频率都在
     * {@link SmsCodeService} 里，这里只负责"校验通过之后"的事。
     */
    public Map<String, Object> login(LoginReq req) {
        // 防刷：同一手机号 60 秒内最多 3 次登录尝试（比验证码错误次数限制更前置一层）
        rateLimitService.check("login", req.phone(), 3, 60);
        // 校验并消费验证码；不通过直接抛业务异常
        smsCodeService.verifyAndConsume(req.phone(), req.code());

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
     * 下发登录验证码。
     *
     * @return 仅开发/演示模式（echo-code=true）带回验证码，方便前端直接展示；
     *         生产环境返回空 map（真正的码只走短信通道）
     */
    public Map<String, Object> sendLoginCode(String phone) {
        // 小时级上限：SmsCodeService 内部已有 60 秒间隔限制（防连点），
        // 这层用来挡"每隔一分钟发一次"的持续骚扰与短信费用攻击。
        // 阈值走配置，别写死在代码里 —— 调试/压测时都要临时调整。
        rateLimitService.check("sms-send", phone, sendHourlyLimit, 3600);
        String devCode = smsCodeService.send(phone);
        Map<String, Object> data = new HashMap<>();
        if (devCode != null) {
            // 明确标注：这是开发/演示专用回显，前端应显著提示用户
            data.put("devCode", devCode);
            data.put("devMode", true);
        }
        return data;
    }
}
