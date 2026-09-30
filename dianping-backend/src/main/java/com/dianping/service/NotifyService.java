package com.dianping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianping.entity.Notify;
import com.dianping.entity.User;
import com.dianping.mapper.NotifyMapper;
import com.dianping.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 站内消息（轻量版）：被赞/被评论/被回复/被关注/被收藏/审核结果。
 * 只做站内红点+列表，不做推送（推送需要外部服务）。
 */
@Service
@RequiredArgsConstructor
public class NotifyService {

    public static final String T_LIKE = "LIKE";
    public static final String T_COMMENT = "COMMENT";
    public static final String T_REPLY = "REPLY";
    public static final String T_FOLLOW = "FOLLOW";
    public static final String T_FAV = "FAV";
    public static final String T_AUDIT_PASS = "AUDIT_PASS";
    public static final String T_AUDIT_REJECT = "AUDIT_REJECT";

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final NotifyMapper notifyMapper;
    private final UserMapper userMapper;

    /** 写一条通知；给自己触发的写；actor=0 表示系统 */
    public void send(Long userId, String type, Long actorId, Long contentId, Long commentId, String text) {
        if (userId == null || (actorId != null && actorId.equals(userId))) {
            return;
        }
        Notify n = new Notify();
        n.setUserId(userId);
        n.setType(type);
        n.setActorId(actorId == null ? 0L : actorId);
        n.setContentId(contentId == null ? 0L : contentId);
        n.setCommentId(commentId == null ? 0L : commentId);
        n.setText(text == null ? "" : text);
        n.setIsRead(0);
        notifyMapper.insert(n);
    }

    /** 未读数 */
    public long unreadCount(Long me) {
        return notifyMapper.selectCount(new LambdaQueryWrapper<Notify>()
                .eq(Notify::getUserId, me)
                .eq(Notify::getIsRead, 0));
    }

    /** 消息列表（时间倒序，组装触发人昵称与内容标题文案） */
    public Map<String, Object> list(Long me, int page, int pageSize) {
        Page<Notify> p = notifyMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<Notify>()
                        .eq(Notify::getUserId, me)
                        .orderByDesc(Notify::getCreateTime));
        List<Long> actorIds = p.getRecords().stream()
                .map(Notify::getActorId).filter(id -> id > 0).distinct().toList();
        Map<Long, User> actors = actorIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(actorIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));

        List<Map<String, Object>> list = new ArrayList<>();
        for (Notify n : p.getRecords()) {
            Map<String, Object> m = new HashMap<>();
            m.put("notifyId", String.valueOf(n.getId()));
            m.put("type", n.getType());
            User actor = actors.get(n.getActorId());
            m.put("actorNickname", actor == null ? "系统" : actor.getNickname());
            m.put("contentId", n.getContentId() == 0 ? "" : String.valueOf(n.getContentId()));
            m.put("text", n.getText());
            m.put("isRead", n.getIsRead());
            m.put("createTime", n.getCreateTime() == null ? "" : n.getCreateTime().format(FMT));
            list.add(m);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("total", p.getTotal());
        data.put("unread", unreadCount(me));
        data.put("list", list);
        return data;
    }

    /** 全部已读 */
    public void readAll(Long me) {
        notifyMapper.update(null, new LambdaUpdateWrapper<Notify>()
                .eq(Notify::getUserId, me)
                .eq(Notify::getIsRead, 0)
                .set(Notify::getIsRead, 1));
    }
}
