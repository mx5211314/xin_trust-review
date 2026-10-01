package com.dianping.module.content;

import com.dianping.common.DianpingConst;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 点赞数落库：Redis 脏集合 -> MySQL，每 5 分钟一次。
 *
 * 设计说明（相对初版的重要修正）：
 * - 初版用"Redis 绝对计数覆盖 content.like_count"，Redis 丢数据会导致计数倒退
 * - 现版：点赞只标记 contentId 到 like:dirty；任务按 user_action（点赞关系表）重新 COUNT
 *   写回 content.like_count —— **关系表是真值源**，Redis 只做"待办清单"，丢了也不影响正确性
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LikeSyncTask {

    private final StringRedisTemplate redis;
    private final ContentMapper contentMapper;

    @Scheduled(fixedDelay = 5 * 60 * 1000, initialDelay = 60 * 1000)
    public void sync() {
        Set<String> dirty;
        try {
            dirty = redis.opsForSet().members(DianpingConst.REDIS_LIKE_DIRTY);
        } catch (Exception e) {
            log.warn("read like:dirty failed: {}", e.getMessage());
            return;
        }
        if (dirty == null || dirty.isEmpty()) {
            return;
        }
        int ok = 0;
        for (String idStr : dirty) {
            try {
                long id = Long.parseLong(idStr);
                contentMapper.syncLikeCountFromActions(id);
                redis.opsForSet().remove(DianpingConst.REDIS_LIKE_DIRTY, idStr);
                ok++;
            } catch (Exception ex) {
                log.warn("sync like_count failed, contentId={}", idStr, ex);
            }
        }
        log.info("like_count synced: {}/{}", ok, dirty.size());
    }
}
