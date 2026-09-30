package com.dianping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianping.common.BizException;
import com.dianping.common.ResultCode;
import com.dianping.dto.ContentVO;
import com.dianping.entity.Content;
import com.dianping.entity.User;
import com.dianping.mapper.ContentMapper;
import com.dianping.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public void approve(Long id) {
        Content c = mustGet(id);
        c.setStatus("APPROVED");
        c.setRejectReason("");
        c.setAuditTime(java.time.LocalDateTime.now());
        contentMapper.updateById(c);
        notifyService.send(c.getUserId(), com.dianping.service.NotifyService.T_AUDIT_PASS, 0L, id, 0L, c.getTitle());
    }

    public void reject(Long id, String reason) {
        Content c = mustGet(id);
        c.setStatus("REJECTED");
        c.setRejectReason(reason);
        c.setAuditTime(java.time.LocalDateTime.now());
        contentMapper.updateById(c);
        notifyService.send(c.getUserId(), com.dianping.service.NotifyService.T_AUDIT_REJECT, 0L, id, 0L, reason);
    }

    public void takedown(Long id) {
        Content c = mustGet(id);
        c.setStatus("TAKEN_DOWN");
        contentMapper.updateById(c);
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

    public void ban(Long userId) {
        User u = mustGetUser(userId);
        u.setStatus("BANNED");
        userMapper.updateById(u);
    }

    public void unban(Long userId) {
        User u = mustGetUser(userId);
        u.setStatus("NORMAL");
        userMapper.updateById(u);
    }

    public void grantReviewer(Long userId) {
        User u = mustGetUser(userId);
        u.setRole("REVIEWER");
        userMapper.updateById(u);
    }

    public void revokeReviewer(Long userId) {
        User u = mustGetUser(userId);
        u.setRole("USER");
        userMapper.updateById(u);
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
