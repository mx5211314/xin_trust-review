package com.dianping.module.auth;

/**
 * 短信发送通道。
 *
 * **为什么抽成接口**：验证码的「机制」和「渠道」是两件事 ——
 * 生成随机码、存 Redis、TTL 过期、一次性消费、错误次数限制、发送频率限制，
 * 这些不依赖任何短信服务商，现在就能做完整；
 * 真正"把短信发出去"才需要服务商资质。
 *
 * 所以这里留一个接口，当前只有 {@link LogSmsSender}（打日志）。
 * 将来拿到资质后新增一个实现类（如 AliyunSmsSender），
 * {@link SmsCodeService} 与 AuthService **不需要任何改动**。
 */
public interface SmsSender {

    /**
     * 发送验证码。
     *
     * 实现方可以抛异常表示发送失败 —— 调用方会据此回滚发送频率锁，
     * 避免"发送失败但用户还要等 60 秒才能重试"。
     */
    void send(String phone, String code);
}
