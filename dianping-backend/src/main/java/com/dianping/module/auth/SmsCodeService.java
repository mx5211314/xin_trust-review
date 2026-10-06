package com.dianping.module.auth;

import com.dianping.common.BizException;
import com.dianping.common.DianpingConst;
import com.dianping.common.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * 短信验证码：生成 / 存储 / 校验 / 消费。
 *
 * 这里只做**机制**，不碰**渠道**（渠道见 {@link SmsSender}）。
 * 机制部分包含四件事，缺一件都会被绕过：
 *   1. 过期     —— 存 Redis 带 TTL，过期即失效（原实现是固定码，永远有效）
 *   2. 一次性   —— 校验通过立即删除，同一码不能重复用
 *   3. 错误限制 —— 累计错 N 次直接作废，防 6 位码暴力枚举
 *   4. 频率限制 —— 同号 N 秒内只能发一次，防短信轰炸/费用攻击
 *
 * 第 4 条用 Redis 的 `SET NX EX` 实现原子占位，
 * 而不是"先 get 再 set"—— 后者在并发下两次请求会同时通过。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsCodeService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;
    private final SmsSender smsSender;

    /** 验证码有效期（秒） */
    @Value("${dianping.sms.code-ttl-seconds:300}")
    private int codeTtlSeconds;

    /** 最多允许输错几次；超出直接作废该验证码 */
    @Value("${dianping.sms.max-verify-attempts:5}")
    private int maxVerifyAttempts;

    /** 同一手机号两次发送的最小间隔（秒） */
    @Value("${dianping.sms.send-interval-seconds:60}")
    private int sendIntervalSeconds;

    /**
     * 是否把验证码直接回显给前端。
     * 仅本机开发/演示可用；**生产必须 false**（等于把验证码告诉了所有人）。
     */
    @Value("${dianping.sms.echo-code:false}")
    private boolean echoCode;

    /**
     * 发送验证码。
     *
     * @return 可直接展示给用户的验证码；**仅 echo-code=true 时非 null**，
     *         生产环境恒为 null（真正的码只会走短信通道）
     */
    public String send(String phone) {
        String sentKey = DianpingConst.REDIS_SMS_SENT + phone;
        try {
            // NX 语义：只有 key 不存在时才写入成功 —— 天然实现"多久内只能发一次"
            Boolean first = redis.opsForValue()
                    .setIfAbsent(sentKey, "1", Duration.ofSeconds(sendIntervalSeconds));
            if (!Boolean.TRUE.equals(first)) {
                throw new BizException(ResultCode.TOO_MANY_REQUESTS,
                        "验证码已发送，请 " + sendIntervalSeconds + " 秒后再试");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            // Redis 不可用时验证码无处存放，只能明确报错，不能假装成功
            log.error("下发验证码失败：Redis 不可用，phone={}", maskPhone(phone), e);
            throw new BizException(ResultCode.SYSTEM_ERROR, "验证码服务暂不可用，请稍后重试");
        }

        String code = randomCode();
        try {
            redis.opsForValue().set(codeKey(phone), code, Duration.ofSeconds(codeTtlSeconds));
            // 发新码时重置错误计数，否则上一次输错的记录会累加到新码上
            redis.delete(attemptKey(phone));
            smsSender.send(phone, code);
        } catch (Exception e) {
            // 发送失败要把频率锁放掉，否则用户被自己的失败发送锁在门外 60 秒
            safeDelete(sentKey);
            log.error("下发验证码失败：phone={}", maskPhone(phone), e);
            throw new BizException(ResultCode.SYSTEM_ERROR, "验证码发送失败，请稍后重试");
        }
        return echoCode ? code : null;
    }

    /**
     * 校验并**消费**验证码（一次性）。
     * 校验不通过会累计错误次数，达到上限直接作废该验证码。
     */
    public void verifyAndConsume(String phone, String code) {
        String key = codeKey(phone);
        String saved;
        try {
            saved = redis.opsForValue().get(key);
        } catch (Exception e) {
            log.error("校验验证码失败：Redis 不可用，phone={}", maskPhone(phone), e);
            throw new BizException(ResultCode.SYSTEM_ERROR, "验证码服务暂不可用，请稍后重试");
        }

        if (saved == null || saved.isBlank()) {
            // 没发过、已过期、或已消费过 —— 对外统一说"已过期"，不区分（避免探测）
            throw new BizException(ResultCode.PARAM_ERROR, "验证码已过期，请重新获取");
        }

        if (!saved.equals(code)) {
            recordWrongAttempt(phone, key);
            throw new BizException(ResultCode.PARAM_ERROR, "验证码错误");
        }

        // 一次性消费：通过即删除，同一验证码不能二次使用
        safeDelete(key);
        safeDelete(attemptKey(phone));
    }

    /** 累计错误次数，达上限直接作废验证码 */
    private void recordWrongAttempt(String phone, String codeKey) {
        String ak = attemptKey(phone);
        try {
            Long n = redis.opsForValue().increment(ak);
            redis.expire(ak, Duration.ofSeconds(codeTtlSeconds));
            if (n != null && n >= maxVerifyAttempts) {
                safeDelete(codeKey);
                safeDelete(ak);
                log.warn("验证码错误次数达上限，已作废：phone={}", maskPhone(phone));
                throw new BizException(ResultCode.PARAM_ERROR, "错误次数过多，请重新获取验证码");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            // 计数失败不阻断流程：本次仍算"验证码错误"，只是没累加
            log.warn("验证码错误计数失败：phone={}", maskPhone(phone), e);
        }
    }

    /** 6 位数字码；用 SecureRandom 而非 Random（后者可被预测） */
    private static String randomCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private static String codeKey(String phone) {
        return DianpingConst.REDIS_SMS_CODE + phone;
    }

    private static String attemptKey(String phone) {
        return DianpingConst.REDIS_SMS_ATTEMPT + phone;
    }

    /** 清理类操作失败不该影响主流程（Redis 可能恰好抖动） */
    private void safeDelete(String key) {
        try {
            redis.delete(key);
        } catch (Exception ignored) {
            // 删不掉最坏情况是该 key 自然过期，无功能影响
        }
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
