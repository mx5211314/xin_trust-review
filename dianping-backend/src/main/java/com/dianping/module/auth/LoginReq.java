package com.dianping.module.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 登录请求。
 *
 * 验证码需先调 `POST /auth/sms/send` 获取（6 位随机码，5 分钟有效、一次性）。
 * 开发/演示模式下该接口会把验证码回显在 data.devCode 里，前端可直接展示。
 */
public record LoginReq(
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不对")
        String phone,
        @NotBlank(message = "验证码不能为空")
        String code) {
}
