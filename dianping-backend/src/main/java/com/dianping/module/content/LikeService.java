package com.dianping.module.content;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dianping.common.BizException;
import com.dianping.common.ResultCode;
import com.dianping.module.content.Content;
import com.dianping.module.interaction.UserAction;
import com.dianping.module.content.ContentMapper;
import com.dianping.module.interaction.UserActionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.dianping.module.notify.NotifyService;

/**
 * 点赞：user_action 表存关系（真值源）+ Redis 脏集合标记待落库。
 * - 幂等：唯一索引 + 先查后插，重复点赞返回 2003
 * - content.like_count 由定时任务按 user_action 真实计数重算（见 LikeSyncTask），
 *   不会因 Redis 数据丢失而倒退
 */
@Service
@RequiredArgsConstructor
public class LikeService {

    private final ContentMapper contentMapper;
    private final UserActionMapper userActionMapper;
    private final StringRedisTemplate redis;
    private final NotifyService notifyService;

    public void like(Long userId, Long contentId) {
        Content c = contentMapper.selectById(contentId);
        if (c == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        boolean exists = userActionMapper.selectCount(new LambdaQueryWrapper<UserAction>()
                .eq(UserAction::getUserId, userId)
                .eq(UserAction::getContentId, contentId)
                .eq(UserAction::getType, UserAction.TYPE_LIKE)) > 0;
        if (exists) {
            throw new BizException(ResultCode.DUPLICATE_ACTION);
        }
        UserAction action = new UserAction();
        action.setUserId(userId);
        action.setContentId(contentId);
        action.setType(UserAction.TYPE_LIKE);
        try {
            userActionMapper.insert(action);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 并发重复点赞：唯一索引兜底
            throw new BizException(ResultCode.DUPLICATE_ACTION);
        }
        markDirty(contentId);
        notifyService.send(c.getUserId(), com.dianping.module.notify.NotifyService.T_LIKE, userId, contentId, 0L, "");
    }

    public void unlike(Long userId, Long contentId) {
        int deleted = userActionMapper.delete(new LambdaQueryWrapper<UserAction>()
                .eq(UserAction::getUserId, userId)
                .eq(UserAction::getContentId, contentId)
                .eq(UserAction::getType, UserAction.TYPE_LIKE));
        if (deleted > 0) {
            markDirty(contentId);
        }
    }

    /**
     * 标记内容点赞数待落库（SyncTask 消费后按 user_action 重算）。
     * Redis 不可用时降级为"立即同步落库"，保证没有 Redis 的环境计数也是准确的。
     */
    private void markDirty(Long contentId) {
        try {
            redis.opsForSet().add(com.dianping.common.DianpingConst.REDIS_LIKE_DIRTY, String.valueOf(contentId));
        } catch (Exception e) {
            // 无 Redis：直接按关系表重算落库（优雅降级）
            try {
                contentMapper.syncLikeCountFromActions(contentId);
            } catch (Exception ignored) {
                // 落库也失败：不影响点赞关系已写入的主流程
            }
        }
    }
}
