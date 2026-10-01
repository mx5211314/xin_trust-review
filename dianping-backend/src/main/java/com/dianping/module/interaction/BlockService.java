package com.dianping.module.interaction;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dianping.common.BizException;
import com.dianping.common.ResultCode;
import com.dianping.module.user.User;
import com.dianping.module.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 拉黑：单向屏蔽，用于信息流/评论/私信隔离。
 * 拉黑是"双向克制"——我拉黑对方后，双方都看不到对方的内容与私信。
 */
@Service
@RequiredArgsConstructor
public class BlockService {

    private final BlockMapper blockMapper;
    private final UserMapper userMapper;

    /** 拉黑（幂等；不能拉黑自己） */
    public void block(Long me, Long targetId) {
        if (me.equals(targetId)) {
            throw new BizException(ResultCode.PARAM_ERROR, "不能拉黑自己");
        }
        if (userMapper.selectById(targetId) == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (isBlocked(me, targetId)) {
            return;
        }
        Block b = new Block();
        b.setUserId(me);
        b.setBlockedId(targetId);
        try {
            blockMapper.insert(b);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 并发重复拉黑：唯一键兜底，视为成功
        }
    }

    /** 取消拉黑 */
    public void unblock(Long me, Long targetId) {
        blockMapper.delete(new LambdaQueryWrapper<Block>()
                .eq(Block::getUserId, me)
                .eq(Block::getBlockedId, targetId));
    }

    /** 我是否拉黑了 target */
    public boolean isBlocked(Long me, Long targetId) {
        if (me == null) return false;
        return blockMapper.selectCount(new LambdaQueryWrapper<Block>()
                .eq(Block::getUserId, me)
                .eq(Block::getBlockedId, targetId)) > 0;
    }

    /** 我拉黑的用户 id 集合 */
    public Set<Long> blockedIds(Long me) {
        if (me == null) return Set.of();
        return blockMapper.selectList(new LambdaQueryWrapper<Block>().eq(Block::getUserId, me))
                .stream().map(Block::getBlockedId).collect(Collectors.toSet());
    }

    /** 拉黑我的用户 id 集合（用于双向克制） */
    public Set<Long> blockedByIds(Long me) {
        if (me == null) return Set.of();
        return blockMapper.selectList(new LambdaQueryWrapper<Block>().eq(Block::getBlockedId, me))
                .stream().map(Block::getUserId).collect(Collectors.toSet());
    }

    /** 信息流/评论中应被屏蔽的作者 id（我拉的 + 拉我的，双向） */
    public Set<Long> hiddenAuthorIds(Long me) {
        Set<Long> s = new HashSet<>(blockedIds(me));
        s.addAll(blockedByIds(me));
        return s;
    }

    /** 我的黑名单列表（含昵称/头像） */
    public List<Map<String, Object>> listBlocks(Long me) {
        Set<Long> ids = blockedIds(me);
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, User> users = userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        return ids.stream().map(id -> {
            User u = users.get(id);
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("userId", String.valueOf(id));
            m.put("nickname", u == null ? "已注销" : u.getNickname());
            m.put("avatar", u == null ? "" : u.getAvatar());
            return m;
        }).toList();
    }
}
