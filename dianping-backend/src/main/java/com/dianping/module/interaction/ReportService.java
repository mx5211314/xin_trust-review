package com.dianping.module.interaction;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianping.common.BizException;
import com.dianping.common.ResultCode;
import com.dianping.module.content.ContentMapper;
import com.dianping.module.comment.CommentMapper;
import com.dianping.module.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 举报：内容 / 评论 / 用户 三类。仅落库 + 供后台审核，不做自动处置。
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportMapper reportMapper;
    private final ContentMapper contentMapper;
    private final CommentMapper commentMapper;
    private final UserMapper userMapper;

    private static final Set<String> TYPES = Set.of("CONTENT", "COMMENT", "USER");

    /** 提交举报（幂等：同一人对同一对象同类型仅留一条 PENDING） */
    public void report(Long me, String targetType, Long targetId, String reason) {
        if (targetType == null || !TYPES.contains(targetType)) {
            throw new BizException(ResultCode.PARAM_ERROR, "举报类型非法");
        }
        if (targetId == null || targetId <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "举报对象无效");
        }
        // 校验对象存在
        boolean exists = switch (targetType) {
            case "CONTENT" -> contentMapper.selectById(targetId) != null;
            case "COMMENT" -> commentMapper.selectById(targetId) != null;
            case "USER" -> userMapper.selectById(targetId) != null;
            default -> false;
        };
        if (!exists) {
            throw new BizException(ResultCode.NOT_FOUND, "举报对象不存在");
        }
        if (reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getReporterId, me)
                .eq(Report::getTargetType, targetType)
                .eq(Report::getTargetId, targetId)
                .eq(Report::getStatus, "PENDING")) > 0) {
            throw new BizException(ResultCode.DUPLICATE_ACTION, "已举报，等待处理");
        }
        Report r = new Report();
        r.setReporterId(me);
        r.setTargetType(targetType);
        r.setTargetId(targetId);
        r.setReason(reason == null ? "" : reason.trim());
        r.setStatus("PENDING");
        reportMapper.insert(r);
    }

    /** 后台：举报列表（按时间倒序） */
    public Map<String, Object> list(int page, int pageSize, String status) {
        Page<Report> p = reportMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<Report>()
                        .eq(status != null && !status.isBlank(), Report::getStatus, status)
                        .orderByDesc(Report::getCreateTime));
        Map<String, Object> data = new HashMap<>();
        data.put("total", p.getTotal());
        data.put("list", p.getRecords());
        return data;
    }

    /** 后台：标记处理 */
    public void handle(Long id, String status) {
        Report r = reportMapper.selectById(id);
        if (r == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        r.setStatus(status);
        reportMapper.updateById(r);
    }
}
