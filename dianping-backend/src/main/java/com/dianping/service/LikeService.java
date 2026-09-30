package com.dianping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dianping.common.BizException;
import com.dianping.common.ResultCode;
import com.dianping.entity.Content;
import com.dianping.entity.UserAction;
import com.dianping.mapper.ContentMapper;
import com.dianping.mapper.UserActionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 点赞：Redis Hash 计数（like:cnt，field=contentId）+ user_action 表存关系。
 * - 幂等：重复点赞返回 2003，不重复计数
 * - like_count 定时任务从 Redis 同步进 content 表（见 task/LikeSyncTask）
 */
@Service
@RequiredArgsConstructor
public class LikeService {

    private static final String COUNT_KEY = "like:cnt";

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
        userActionMapper.insert(action);
        redis.opsForHash().increment(COUNT_KEY, String.valueOf(contentId), 1);
        notifyService.send(c.getUserId(), com.dianping.service.NotifyService.T_LIKE, userId, contentId, 0L, "");
    }

    public void unlike(Long userId, Long contentId) {
        int deleted = userActionMapper.delete(new LambdaQueryWrapper<UserAction>()
                .eq(UserAction::getUserId, userId)
                .eq(UserAction::getContentId, contentId)
                .eq(UserAction::getType, UserAction.TYPE_LIKE));
        if (deleted > 0) {
            redis.opsForHash().increment(COUNT_KEY, String.valueOf(contentId), -1);
        }
    }
}
