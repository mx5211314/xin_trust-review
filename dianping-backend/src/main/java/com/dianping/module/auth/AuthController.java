package com.dianping.module.auth;

import com.dianping.common.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /auth/sms/send  body: {"phone":"13800000000"}
     * 下发登录验证码。开发/演示模式会在 data.devCode 里带回验证码供前端展示。
     */
    @PostMapping("/sms/send")
    public R<Map<String, Object>> sendSmsCode(@Valid @RequestBody SmsSendReq req) {
        return R.ok(authService.sendLoginCode(req.phone()));
    }

    /** POST /auth/login  body: {"phone":"...","code":"123456"} */
    @PostMapping("/login")
    public R<Map<String, Object>> login(@Valid @RequestBody LoginReq req) {
        return R.ok(authService.login(req));
    }
}
