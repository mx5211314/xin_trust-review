package com.dianping.module.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianping.common.BizException;
import com.dianping.common.ResultCode;
import com.dianping.module.content.ContentVO;
import com.dianping.module.content.Content;
import com.dianping.module.user.User;
import com.dianping.module.content.ContentMapper;
import com.dianping.module.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.dianping.module.content.ContentService;
import com.dianping.module.notify.NotifyService;

/**
 * 后台管理：内容审核、下架、用户封禁、点评人授权。
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final ContentMapper contentMapper;
    private final UserMapper userMapper;
    private final ContentService contentService;
    private final NotifyService notifyService;
    private final org.springframework.data.redis.core.StringRedisTemplate redis;

    public void approve(Long id) {
        Content c = mustGet(id);
        c.setStatus("APPROVED");
        c.setRejectReason("");
        c.setAuditTime(java.time.LocalDateTime.now());
        contentMapper.updateById(c);
        notifyService.send(c.getUserId(), com.dianping.module.notify.NotifyService.T_AUDIT_PASS, 0L, id, 0L, c.getTitle());
    }

    public void reject(Long id, String reason) {
        Content c = mustGet(id);
        c.setStatus("REJECTED");
        c.setRejectReason(reason);
        c.setAuditTime(java.time.LocalDateTime.now());
        contentMapper.updateById(c);
        notifyService.send(c.getUserId(), com.dianping.module.notify.NotifyService.T_AUDIT_REJECT, 0L, id, 0L, reason);
    }

    public void takedown(Long id) {
        Content c = mustGet(id);
        c.setStatus("TAKEN_DOWN");
        contentMapper.updateById(c);
    }

    /** 后台统计头：待审 / 今日通过 / 今日驳回 / 驳回率（管理台重构用） */
    public Map<String, Object> adminStats() {
        long pending = contentMapper.selectCount(new LambdaQueryWrapper<Content>()
                .eq(Content::getStatus, "PENDING"));
        java.time.LocalDateTime start = java.time.LocalDate.now().atStartOfDay();
        long todayPassed = contentMapper.selectCount(new LambdaQueryWrapper<Content>()
                .eq(Content::getStatus, "APPROVED")
                .ge(Content::getAuditTime, start));
        long todayRejected = contentMapper.selectCount(new LambdaQueryWrapper<Content>()
                .eq(Content::getStatus, "REJECTED")
                .ge(Content::getAuditTime, start));
        long decided = todayPassed + todayRejected;
        double rate = decided == 0 ? 0 : (todayRejected * 100.0 / decided);
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("pending", pending);
        m.put("todayPassed", todayPassed);
        m.put("todayRejected", todayRejected);
        m.put("rejectRate", Math.round(rate * 10) / 10.0);
        return m;
    }

    public Object userList(String keyword, int page, int pageSize) {
        Page<User> p = userMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<User>()
                        .and(keyword != null && !keyword.isBlank(),
                                w -> w.like(User::getPhone, keyword).or().like(User::getNickname, keyword))
                        .orderByDesc(User::getCreateTime));
        p.getRecords().forEach(u -> u.setPhone(maskPhone(u.getPhone())));  // 列表脱敏
        return p;
    }

    /** 清掉用户状态缓存，使封禁/解封/授权立即生效（否则最长 60s 延迟） */
    private void evictUserCache(Long userId) {
        try {
            redis.delete(com.dianping.common.DianpingConst.REDIS_USER_STATUS + userId);
        } catch (Exception ignored) {
            // Redis 不可用：等缓存自然过期
        }
    }

    // 下面四个方法都会改写缓存的 status|role，必须同步失效缓存。
    // 否则 AuthInterceptor 最长 60 秒内还按旧状态放行 ——
    // 封禁了还能继续操作、撤销点评人后还能继续发布。

    public void ban(Long userId) {
        User u = mustGetUser(userId);
        u.setStatus("BANNED");
        userMapper.updateById(u);
        evictUserCache(userId);
    }

    public void unban(Long userId) {
        User u = mustGetUser(userId);
        u.setStatus("NORMAL");
        userMapper.updateById(u);
        evictUserCache(userId);
    }

    public void grantReviewer(Long userId) {
        User u = mustGetUser(userId);
        u.setRole("REVIEWER");
        userMapper.updateById(u);
        evictUserCache(userId);
    }

    public void revokeReviewer(Long userId) {
        User u = mustGetUser(userId);
        u.setRole("USER");
        userMapper.updateById(u);
        evictUserCache(userId);
    }

    // ---------- 内部 ----------

    private Content mustGet(Long id) {
        Content c = contentMapper.selectById(id);
        if (c == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return c;
    }

    private User mustGetUser(Long id) {
        User u = userMapper.selectById(id);
        if (u == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return u;
    }

    /** 手机号脱敏：138****0000 */
    private String maskPhone(String phone) {
        return phone == null || phone.length() < 11 ? phone
                : phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
