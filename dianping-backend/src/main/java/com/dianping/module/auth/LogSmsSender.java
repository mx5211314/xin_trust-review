package com.dianping.module.auth;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 默认短信通道：**不真的发短信，只把验证码打进日志**。
 *
 * 这是当前唯一实现。有它存在时，本机开发能跑通完整登录流程
 * （配合 `dianping.sms.echo-code=true` 把验证码回显给前端）。
 *
 * TODO 接入真实短信服务商（阿里云/腾讯云）时：
 *   1. 新增 `AliyunSmsSender implements SmsSender`
 *   2. 用 `@ConditionalOnProperty(name = "dianping.sms.provider", havingValue = "aliyun")`
 *      与 `@ConditionalOnMissingBean` 控制装配
 *   3. SmsCodeService / AuthService 一行都不用改
 */
@Slf4j
@Component
public class LogSmsSender implements SmsSender {

    @Value("${dianping.sms.echo-code:false}")
    private boolean echoCode;

    /**
     * 启动时把"能不能真的收到验证码"这件事说清楚。
     *
     * 只打日志的通道 + 不开 echo-code = 谁都无法登录。
     * 这种"安全地不可用"状态必须显式告警，否则上线后会以为登录被人改坏了。
     */
    @PostConstruct
    void warnIfLoginUnusable() {
        if (echoCode) {
            log.warn("【短信】当前为日志通道 + 验证码回显（仅限本机开发/演示）。"
                    + "生产环境必须接入真实短信服务商，并把 dianping.sms.echo-code 置为 false。");
        } else {
            log.error("【短信】当前为日志通道且未开启 echo-code："
                    + "任何手机号都收不到验证码，**登录将不可用**。"
                    + "本机开发请用 --spring.profiles.active=dev 启动；"
                    + "正式上线请实现真实的 SmsSender。");
        }
    }

    @Override
    public void send(String phone, String code) {
        log.warn("[短信未接入·仅打日志] 向 {} 发送验证码 {}", maskPhone(phone), code);
    }

    /** 日志脱敏：手机号属个人信息 */
    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
