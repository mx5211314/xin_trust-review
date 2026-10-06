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
    /** 内容可见性统一判定（点赞也要拦下架/待审笔记） */
    private final ContentAccess contentAccess;

    public void like(Long userId, Long contentId) {
        // 可见性校验（原来只判"存在"，能给下架/待审笔记点赞）
        Content c = contentAccess.require(userId, contentId);
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
     * 点赞后同步计数：标记脏集合（对账兜底）+ 立即按 user_action 重算落库（保证前端即时看到）。
     *
     * 设计取舍：当前数据量下"实时落库"体验最好；
     * 若点赞 QPS 变高（主表写热点），改为只标记脏集合、由 LikeSyncTask 异步落库，
     * 前端计数改读 Redis 即可（两边都是增量/真值源，不会倒退）。
     */
    private void markDirty(Long contentId) {
        try {
            redis.opsForSet().add(com.dianping.common.DianpingConst.REDIS_LIKE_DIRTY, String.valueOf(contentId));
        } catch (Exception ignored) {
            // Redis 不可用：不影响，下面仍会同步落库
        }
        try {
            contentMapper.syncLikeCountFromActions(contentId);
        } catch (Exception ignored) {
            // 落库失败：点赞关系已写入，dirty 集合/SyncTask 后续会修正
        }
    }
}
