package com.dianping.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 登录请求。dev 环境验证码固定 8888，见 AuthService。
 */
public record LoginReq(
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不对")
        String phone,
        @NotBlank(message = "验证码不能为空")
        String code) {
}
