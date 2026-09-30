package com.dianping.task;

import com.dianping.mapper.ContentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 点赞数同步：Redis(实时计数) -> MySQL(落库)，每 5 分钟一次。
 *
 * 设计说明（对应设计笔记）：
 * - 写路径：点赞/取消只写 Redis 和 user_action 表，不直接改 content.like_count，
 *   高频写不压在内容主表上
 * - 读路径：like_count 字段允许有最长 5 分钟延迟（老板已认可）
 * - Redis 挂了：点赞功能不可用（fail fast），落库数据不丢，可接受
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LikeSyncTask {

    private static final String COUNT_KEY = "like:cnt";

    private final StringRedisTemplate redis;
    private final ContentMapper contentMapper;

    @Scheduled(fixedDelay = 5 * 60 * 1000, initialDelay = 60 * 1000)
    public void sync() {
        Map<Object, Object> all = redis.opsForHash().entries(COUNT_KEY);
        if (all.isEmpty()) {
            return;
        }
        int ok = 0;
        for (Map.Entry<Object, Object> e : all.entrySet()) {
            try {
                long contentId = Long.parseLong(e.getKey().toString());
                long count = Long.parseLong(e.getValue().toString());
                contentMapper.updateLikeCount(contentId, count);
                ok++;
            } catch (Exception ex) {
                log.warn("sync like_count failed, contentId={}, val={}", e.getKey(), e.getValue(), ex);
            }
        }
        log.info("like_count synced: {}/{}", ok, all.size());
    }
}
