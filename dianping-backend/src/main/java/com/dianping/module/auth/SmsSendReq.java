package com.dianping.module.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 发送登录验证码请求 */
public record SmsSendReq(
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不对")
        String phone) {
}
