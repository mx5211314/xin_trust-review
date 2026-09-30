package com.dianping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianping.common.BizException;
import com.dianping.common.ResultCode;
import com.dianping.dto.ContentVO;
import com.dianping.entity.Content;
import com.dianping.entity.Follow;
import com.dianping.entity.User;
import com.dianping.mapper.FollowMapper;
import com.dianping.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 关注关系：点评人模式的核心闭环——用户关注点评人，首页"关注"tab 只看 TA 们的更新。
 */
@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowMapper followMapper;
    private final UserMapper userMapper;
    private final ContentService contentService;
    private final NotifyService notifyService;

    /** 关注（幂等：重复关注返回 2003；不能关注自己） */
    public void follow(Long me, Long targetId) {
        if (me.equals(targetId)) {
            throw new BizException(ResultCode.PARAM_ERROR, "不能关注自己");
        }
        if (userMapper.selectById(targetId) == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (isFollowing(me, targetId)) {
            throw new BizException(ResultCode.DUPLICATE_ACTION);
        }
        Follow f = new Follow();
        f.setUserId(me);
        f.setFollowUserId(targetId);
        followMapper.insert(f);
        notifyService.send(targetId, com.dianping.service.NotifyService.T_FOLLOW, me, 0L, 0L, "");
    }

    /** 取关 */
    public void unfollow(Long me, Long targetId) {
        followMapper.delete(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, me)
                .eq(Follow::getFollowUserId, targetId));
    }

    public boolean isFollowing(Long me, Long targetId) {
        if (me == null) return false;
        return followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, me)
                .eq(Follow::getFollowUserId, targetId)) > 0;
    }

    /** TA 的粉丝数 */
    public long followersCount(Long userId) {
        return followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowUserId, userId));
    }

    /** 我关注的用户 id 列表 */
    public List<Long> followingIds(Long me) {
        return followMapper.selectList(new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getUserId, me))
                .stream().map(Follow::getFollowUserId).toList();
    }

    /** 我关注的列表（含昵称） */
    public Map<String, Object> followingList(Long me, int page, int pageSize) {
        Page<Follow> p = followMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getUserId, me)
                        .orderByDesc(Follow::getCreateTime));
        List<Long> ids = p.getRecords().stream().map(Follow::getFollowUserId).toList();
        Map<Long, User> users = ids.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(ids).stream()
                        .collect(java.util.stream.Collectors.toMap(User::getId, u -> u));
        List<Map<String, Object>> list = ids.stream()
                .map(id -> {
                    User u = users.get(id);
                    Map<String, Object> m = new HashMap<>();
                    m.put("userId", String.valueOf(id));
                    m.put("nickname", u == null ? "已注销" : u.getNickname());
                    m.put("role", u == null ? "" : u.getRole());
                    return m;
                }).toList();
        Map<String, Object> data = new HashMap<>();
        data.put("total", p.getTotal());
        data.put("list", list);
        return data;
    }

    /** 关注 feed：我关注的人的笔记（仅上架，时间倒序） */
    public Map<String, Object> followFeed(Long me, int page, int pageSize) {
        List<Long> ids = followingIds(me);
        if (ids.isEmpty()) {
            Map<String, Object> data = new HashMap<>();
            data.put("total", 0);
            data.put("list", List.of());
            return data;
        }
        return contentService.contentsOfUsers(me, ids, page, pageSize);
    }
}
