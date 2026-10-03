package com.dianping.module.chat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianping.common.BizException;
import com.dianping.common.ResultCode;
import com.dianping.module.chat.Message;
import com.dianping.module.user.User;
import com.dianping.module.chat.MessageMapper;
import com.dianping.module.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.dianping.module.notify.NotifyService;
import com.dianping.module.oss.OssService;

/**
 * 私信：用户之间一对一聊天（点评人模式下的"粉丝问店铺地址"场景）。
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final NotifyService notifyService;
    private final com.dianping.common.RateLimitService rateLimitService;
    private final com.dianping.module.interaction.BlockService blockService;
    private final OssService ossService;

    /** 发私信 */
    public Map<String, Object> send(Long me, Long toUserId, String text, String imageKey) {
        if (toUserId == null || toUserId.equals(me)) {
            throw new BizException(ResultCode.PARAM_ERROR, "不能给自己发私信");
        }
        User to = userMapper.selectById(toUserId);
        if (to == null) {
            throw new BizException(ResultCode.NOT_FOUND, "对方不存在");
        }
        // 拉黑隔离：双向——我拉黑了对方，或对方拉黑了我，均不可发私信
        if (blockService.isBlocked(me, toUserId) || blockService.isBlocked(toUserId, me)) {
            throw new BizException(ResultCode.FORBIDDEN, "对方已屏蔽，无法发送私信");
        }
        String t = text == null ? "" : text.trim();
        String key = imageKey == null ? "" : imageKey.trim();
        // 文本和图片至少要有一样
        if (t.isEmpty() && key.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR, "消息不能为空");
        }
        if (t.length() > 500) {
            throw new BizException(ResultCode.PARAM_ERROR, "消息最多 500 字");
        }
        // 限流：60 秒内最多 20 条私信
        rateLimitService.check("chat", String.valueOf(me), 20, 60);
        Message m = new Message();
        m.setFromUserId(me);
        m.setToUserId(toUserId);
        m.setText(t);
        m.setImageKey(key.isEmpty() ? null : key);
        m.setIsRead(0);
        messageMapper.insert(m);
        // 站内通知提醒对方（纯图片消息用「图片」占位）
        String notice = t.isEmpty() ? "「图片」" : (t.length() > 30 ? t.substring(0, 30) + "…" : t);
        notifyService.send(toUserId, "MESSAGE", me, 0L, 0L, notice);
        // 回传图片可直接访问的 URL，前端本地回显不用再拼
        Map<String, Object> out = new HashMap<>();
        out.put("messageId", String.valueOf(m.getId()));
        out.put("imageUrl", key.isEmpty() ? "" : ossService.publicUrl(key));
        return out;
    }

    /** 与某人的聊天记录（正序；同时把对方发来的置为已读） */
    public Map<String, Object> messages(Long me, Long peerId, int page, int pageSize) {
        User peer = userMapper.selectById(peerId);
        if (peer == null) {
            throw new BizException(ResultCode.NOT_FOUND, "对方不存在");
        }
        // 标记已读
        messageMapper.update(null, new LambdaUpdateWrapper<Message>()
                .eq(Message::getFromUserId, peerId)
                .eq(Message::getToUserId, me)
                .eq(Message::getIsRead, 0)
                .set(Message::getIsRead, 1));
        // 查双方消息（倒序分页后反转成正序）
        Page<Message> p = messageMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<Message>()
                        .and(w -> w.eq(Message::getFromUserId, me).eq(Message::getToUserId, peerId))
                        .or(w -> w.eq(Message::getFromUserId, peerId).eq(Message::getToUserId, me))
                        .orderByDesc(Message::getId));
        List<Map<String, Object>> list = new ArrayList<>();
        for (Message m : p.getRecords()) {
            Map<String, Object> mm = new HashMap<>();
            mm.put("messageId", String.valueOf(m.getId()));
            mm.put("mine", m.getFromUserId().equals(me));
            mm.put("text", m.getText());
            // 图片消息：下发给可直接访问的 URL，纯文本为空串
            mm.put("imageUrl",
                    m.getImageKey() == null || m.getImageKey().isBlank()
                            ? "" : ossService.publicUrl(m.getImageKey()));
            mm.put("createTime", m.getCreateTime() == null ? "" : m.getCreateTime().format(FMT));
            list.add(mm);
        }
        java.util.Collections.reverse(list);
        Map<String, Object> data = new HashMap<>();
        data.put("total", p.getTotal());
        data.put("peer", Map.of(
                "userId", String.valueOf(peer.getId()),
                "nickname", peer.getNickname(),
                "avatar", peer.getAvatar()));
        data.put("list", list);
        return data;
    }

    /** 会话列表（私信 tab；排除拉黑对象，双向） */
    public Map<String, Object> conversations(Long me, int limit) {
        com.dianping.module.interaction.BlockService bs = this.blockService;
        java.util.Set<Long> hidden = bs.hiddenAuthorIds(me);
        List<Map<String, Object>> rows = messageMapper.conversations(me, limit);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Long peerId = ((Number) row.get("peer_id")).longValue();
            if (hidden.contains(peerId)) {
                continue; // 拉黑隔离：不展示与被屏蔽者的会话
            }
            Long lastId = ((Number) row.get("last_id")).longValue();
            User peer = userMapper.selectById(peerId);
            Message last = messageMapper.selectById(lastId);
            if (peer == null || last == null) {
                continue;
            }
            Map<String, Object> c = new HashMap<>();
            c.put("peerId", String.valueOf(peerId));
            c.put("nickname", peer.getNickname());
            c.put("avatar", peer.getAvatar());
            // 会话预览：纯图片消息显示「[图片]」
            boolean hasImg = last.getImageKey() != null && !last.getImageKey().isBlank();
            String preview = hasImg && (last.getText() == null || last.getText().isBlank())
                    ? "[图片]" : last.getText();
            c.put("lastText", preview);
            c.put("lastMine", last.getFromUserId().equals(me));
            c.put("lastTime", last.getCreateTime() == null ? "" : last.getCreateTime().format(FMT));
            c.put("unread", messageMapper.unreadFrom(peerId, me));
            list.add(c);
        }
        long unreadTotal = list.stream().mapToLong(x -> ((Number) x.get("unread")).longValue()).sum();
        Map<String, Object> data = new HashMap<>();
        data.put("unreadTotal", unreadTotal);
        data.put("list", list);
        return data;
    }

    /** 未读私信总数（消息入口红点用） */
    public long unreadTotal(Long me) {
        return messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getToUserId, me)
                .eq(Message::getIsRead, 0));
    }
}
