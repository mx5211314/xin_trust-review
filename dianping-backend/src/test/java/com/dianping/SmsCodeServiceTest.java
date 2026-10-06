package com.dianping;

import com.dianping.common.BizException;
import com.dianping.common.DianpingConst;
import com.dianping.common.ResultCode;
import com.dianping.module.auth.SmsCodeService;
import com.dianping.module.auth.SmsSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 验证码的四条机制：过期 / 一次性 / 错误次数 / 发送频率。
 *
 * 这四条缺一条就能被绕过，所以逐条覆盖 ——
 * 尤其"一次性"和"错误次数"，只靠人工点击很难验到边界。
 */
@ExtendWith(MockitoExtension.class)
class SmsCodeServiceTest {

    private static final String PHONE = "13800000000";
    private static final String CODE_KEY = DianpingConst.REDIS_SMS_CODE + PHONE;
    private static final String ATTEMPT_KEY = DianpingConst.REDIS_SMS_ATTEMPT + PHONE;
    private static final String SENT_KEY = DianpingConst.REDIS_SMS_SENT + PHONE;

    @Mock
    StringRedisTemplate redis;
    @Mock
    ValueOperations<String, String> ops;
    @Mock
    SmsSender smsSender;

    @InjectMocks
    SmsCodeService smsCodeService;

    /** 三个 @Value 字段由 Spring 注入，单测里手动补上 */
    private void config(int ttl, int maxAttempts, int interval, boolean echoCode) {
        ReflectionTestUtils.setField(smsCodeService, "codeTtlSeconds", ttl);
        ReflectionTestUtils.setField(smsCodeService, "maxVerifyAttempts", maxAttempts);
        ReflectionTestUtils.setField(smsCodeService, "sendIntervalSeconds", interval);
        ReflectionTestUtils.setField(smsCodeService, "echoCode", echoCode);
    }

    // ---------- 发送 ----------

    @Test
    @DisplayName("发送成功：写入 Redis 并带 TTL，重置错误计数，调用通道")
    void sendOk() {
        config(300, 5, 60, true);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.setIfAbsent(eq(SENT_KEY), anyString(), any(Duration.class))).thenReturn(true);

        String devCode = smsCodeService.send(PHONE);

        // 6 位数字
        assertNotNull(devCode);
        assertTrue(devCode.matches("\\d{6}"), "验证码应为 6 位数字，实际：" + devCode);
        // 存码时带 TTL（过期机制）
        verify(ops).set(eq(CODE_KEY), eq(devCode), any(Duration.class));
        // 发新码重置错误计数
        verify(redis).delete(ATTEMPT_KEY);
        // 真正调了发送通道
        verify(smsSender).send(PHONE, devCode);
    }

    @Test
    @DisplayName("非开发模式：不回显验证码（生产不能把码告诉调用方）")
    void sendDoesNotEchoWhenNotDev() {
        config(300, 5, 60, false);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.setIfAbsent(eq(SENT_KEY), anyString(), any(Duration.class))).thenReturn(true);

        assertNull(smsCodeService.send(PHONE));
    }

    @Test
    @DisplayName("发送频率限制：N 秒内重复发送返回 1029（靠 SET NX 原子占位）")
    void sendTooFrequent() {
        config(300, 5, 60, true);
        when(redis.opsForValue()).thenReturn(ops);
        // false = key 已存在 = 刚发过
        when(ops.setIfAbsent(eq(SENT_KEY), anyString(), any(Duration.class))).thenReturn(false);

        BizException e = assertThrows(BizException.class, () -> smsCodeService.send(PHONE));
        assertEquals(ResultCode.TOO_MANY_REQUESTS.getCode(), e.getCode().getCode());
        verify(smsSender, never()).send(anyString(), anyString());
    }

    // ---------- 校验 ----------

    @Test
    @DisplayName("验证码不存在或已过期：返回参数错误，提示重新获取")
    void verifyExpired() {
        config(300, 5, 60, true);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get(CODE_KEY)).thenReturn(null);

        BizException e = assertThrows(BizException.class,
                () -> smsCodeService.verifyAndConsume(PHONE, "123456"));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), e.getCode().getCode());
        assertTrue(e.getMessage().contains("过期"));
    }

    @Test
    @DisplayName("验证码正确：一次性消费，校验后立即删除")
    void verifyConsumesOnce() {
        config(300, 5, 60, true);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get(CODE_KEY)).thenReturn("654321");

        assertDoesNotThrow(() -> smsCodeService.verifyAndConsume(PHONE, "654321"));

        // 关键：通过后必须删掉，否则同一个码能反复用
        verify(redis).delete(CODE_KEY);
        verify(redis).delete(ATTEMPT_KEY);
    }

    @Test
    @DisplayName("验证码错误：累计错误次数，未超上限时只报验证码错误")
    void verifyWrongCode() {
        config(300, 5, 60, true);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get(CODE_KEY)).thenReturn("111111");
        when(ops.increment(ATTEMPT_KEY)).thenReturn(1L);

        BizException e = assertThrows(BizException.class,
                () -> smsCodeService.verifyAndConsume(PHONE, "222222"));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), e.getCode().getCode());
        assertEquals("验证码错误", e.getMessage());
        // 未通过时不能删码（否则用户输错一次就得重新获取）
        verify(redis, never()).delete(CODE_KEY);
    }

    @Test
    @DisplayName("错误次数达上限：作废验证码，要求重新获取（防暴力枚举）")
    void verifyTooManyAttempts() {
        config(300, 5, 60, true);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get(CODE_KEY)).thenReturn("111111");
        when(ops.increment(ATTEMPT_KEY)).thenReturn(5L);   // 已达上限

        BizException e = assertThrows(BizException.class,
                () -> smsCodeService.verifyAndConsume(PHONE, "222222"));
        assertTrue(e.getMessage().contains("次数过多"));
        // 必须把码删掉 —— 否则攻击者可以继续猜剩下的组合
        verify(redis).delete(CODE_KEY);
        verify(redis).delete(ATTEMPT_KEY);
    }

    @Test
    @DisplayName("Redis 不可用：明确报服务不可用，不吞异常也不假装成功")
    void redisDown() {
        config(300, 5, 60, true);
        when(redis.opsForValue()).thenThrow(new RuntimeException("Connection refused"));

        BizException e = assertThrows(BizException.class, () -> smsCodeService.send(PHONE));
        assertEquals(ResultCode.SYSTEM_ERROR.getCode(), e.getCode().getCode());
        assertTrue(e.getMessage().contains("验证码服务"));
    }
}
