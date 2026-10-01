package com.dianping.common;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 轻量限流：Redis 固定窗口计数。
 * 例：60 秒内同一用户最多发 10 条评论。
 * 说明：Redis 不可用时放行（fail-open）——限流是保护措施，不应把主流程拖挂。
 */
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redis;

    public void check(String biz, String key, int limit, int seconds) {
        try {
            String k = DianpingConst.REDIS_RATE + biz + ":" + key;
            Long n = redis.opsForValue().increment(k);
            if (n != null && n == 1L) {
                redis.expire(k, Duration.ofSeconds(seconds));
            }
            if (n != null && n > limit) {
                throw new BizException(ResultCode.TOO_MANY_REQUESTS);
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception ignored) {
            // Redis 异常：放行
        }
    }
}
