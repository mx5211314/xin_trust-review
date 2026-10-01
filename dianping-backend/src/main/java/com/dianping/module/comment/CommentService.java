package com.dianping.module.comment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianping.common.BizException;
import com.dianping.common.DianpingConst;
import com.dianping.common.RateLimitService;
import com.dianping.common.ResultCode;
import com.dianping.module.content.Content;
import com.dianping.module.content.ContentMapper;
import com.dianping.module.notify.NotifyService;
import com.dianping.module.user.User;
import com.dianping.module.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.dianping.module.content.ContentService;

/**
 * 评论域：发布（支持楼中楼回复）、列表（父评论 + 子回复两级树）、点赞、删除。
 * 从原 ContentService 拆出，内容域与评论域各自独立演进。
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CommentMapper commentMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final ContentMapper contentMapper;
    private final UserMapper userMapper;
    private final NotifyService notifyService;
    private final RateLimitService rateLimitService;

    /** 评论总数（详情展示用） */
    public long countOf(Long contentId) {
        return commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getContentId, contentId));
    }

    /** 删除内容时级联清理评论 */
    public void deleteByContent(Long contentId) {
        commentMapper.delete(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getContentId, contentId));
    }

/**
     * 评论列表（小红书式）：顶级评论按时间正序分页，子回复挂在父评论的 replies 里。
     * total = 全部评论数（含回复）。
     */
    public Map<String, Object> commentsOf(Long contentId, int page, int pageSize) {
        // 1. 顶级评论（正序：早的在前，新评论追加在底部，符合对话习惯）
        Page<Comment> p = commentMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getContentId, contentId)
                        .eq(Comment::getParentId, 0L)
                        .orderByDesc(Comment::getLikeCount)
                        .orderByAsc(Comment::getId));
        List<Comment> roots = p.getRecords();
        List<Long> rootIds = roots.stream().map(Comment::getId).toList();

        // 2. 子回复：一次 in 查询，正序
        List<Comment> replies = rootIds.isEmpty() ? java.util.List.of()
                : commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getContentId, contentId)
                        .in(Comment::getParentId, rootIds)
                        .orderByAsc(Comment::getId));

        // 3. 收集所有涉及的用户 id，批量查昵称
        List<Long> uids = java.util.stream.Stream.concat(
                java.util.stream.Stream.concat(roots.stream(), replies.stream())
                        .map(Comment::getUserId),
                replies.stream().map(Comment::getReplyToUserId))
                .filter(id -> id != null && id > 0)
                .distinct().toList();
        Map<Long, User> users = uids.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(uids).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));

        // 4. 子回复按 parentId 归组
        Map<Long, List<Comment>> replyGroup = replies.stream()
                .collect(Collectors.groupingBy(Comment::getParentId));

        // 5. 组装：父 + replies
        List<Map<String, Object>> list = new java.util.ArrayList<>();
        for (Comment r : roots) {
            list.add(commentVo(r, null, users));
            List<Comment> rs = replyGroup.getOrDefault(r.getId(), java.util.List.of());
            ((Map<String, Object>) list.get(list.size() - 1)).put("replyCount", rs.size());
            List<Map<String, Object>> rlist = new java.util.ArrayList<>();
            for (Comment c : rs) {
                User replyTo = c.getReplyToUserId() != null && c.getReplyToUserId() > 0
                        ? users.get(c.getReplyToUserId()) : null;
                Map<String, Object> cm = commentVo(c, replyTo, users);
                cm.put("replyToNickname", replyTo == null ? "" : replyTo.getNickname());
                rlist.add(cm);
            }
            ((Map<String, Object>) list.get(list.size() - 1)).put("replies", rlist);
        }

        long totalAll = commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getContentId, contentId));
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("total", totalAll);
        data.put("rootTotal", p.getTotal());
        data.put("list", list);
        return data;
    }

/** 发布评论（支持回复：传 parentId 时自动带上被回复人） */
    public Map<String, Object> addComment(Long contentId, Long me, String text, Long parentId) {
        if (contentMapper.selectById(contentId) == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        // 限流：60 秒内最多 10 条评论
        rateLimitService.check("comment", String.valueOf(me), 10, 60);
        Comment c = new Comment();
        c.setContentId(contentId);
        c.setUserId(me);
        c.setText(text);
        Long replyToUser = 0L;
        if (parentId != null && parentId > 0) {
            Comment parent = commentMapper.selectById(parentId);
            if (parent == null || !parent.getContentId().equals(contentId)) {
                throw new BizException(ResultCode.PARAM_ERROR, "被回复的评论不存在");
            }
            // 楼中楼统一挂在根评论下；被回复人 = 被回复那条评论的作者
            Long rootId = parent.getParentId() != null && parent.getParentId() > 0
                    ? parent.getParentId() : parent.getId();
            c.setParentId(rootId);
            c.setReplyToUserId(parent.getUserId());
            replyToUser = parent.getUserId();
        } else {
            c.setParentId(0L);
            c.setReplyToUserId(0L);
        }
        commentMapper.insert(c);
        // 通知：作者收到评论；回复时被回复人也收到一条
        notifyService.send(contentMapper.selectById(contentId).getUserId(),
                NotifyService.T_COMMENT, me, contentId, c.getId(), text);
        if (replyToUser > 0 && !replyToUser.equals(contentMapper.selectById(contentId).getUserId())) {
            notifyService.send(replyToUser, NotifyService.T_REPLY, me, contentId, c.getId(), text);
        }
        return Map.of("commentId", String.valueOf(c.getId()));
    }

/** 评论点赞（幂等：重复返回 2003） */
    public void likeComment(Long commentId, Long me) {
        Comment c = commentMapper.selectById(commentId);
        if (c == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        boolean exists = commentLikeMapper.selectCount(
                new LambdaQueryWrapper<com.dianping.module.comment.CommentLike>()
                        .eq(com.dianping.module.comment.CommentLike::getCommentId, commentId)
                        .eq(com.dianping.module.comment.CommentLike::getUserId, me)) > 0;
        if (exists) {
            throw new BizException(ResultCode.DUPLICATE_ACTION);
        }
        com.dianping.module.comment.CommentLike cl = new com.dianping.module.comment.CommentLike();
        cl.setCommentId(commentId);
        cl.setUserId(me);
        commentLikeMapper.insert(cl);
        c.setLikeCount((c.getLikeCount() == null ? 0 : c.getLikeCount()) + 1);
        commentMapper.updateById(c);
    }

/** 取消评论点赞 */
    public void unlikeComment(Long commentId, Long me) {
        int deleted = commentLikeMapper.delete(
                new LambdaQueryWrapper<com.dianping.module.comment.CommentLike>()
                        .eq(com.dianping.module.comment.CommentLike::getCommentId, commentId)
                        .eq(com.dianping.module.comment.CommentLike::getUserId, me));
        if (deleted > 0) {
            Comment c = commentMapper.selectById(commentId);
            if (c != null) {
                c.setLikeCount(Math.max(0, (c.getLikeCount() == null ? 0 : c.getLikeCount()) - 1));
                commentMapper.updateById(c);
            }
        }
    }

/** 删除自己的评论（管理员可删任意）：逻辑删除 */
    public void deleteComment(Long commentId, Long me, boolean isAdmin) {
        Comment c = commentMapper.selectById(commentId);
        if (c == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        if (!isAdmin && !c.getUserId().equals(me)) {
            throw new BizException(ResultCode.FORBIDDEN, "只能删除自己的评论");
        }
        commentMapper.deleteById(commentId);
    }

/** 评论 VO 组装（扁平 map）；replyTo 传入时带"回复 @"关系，否则为顶级评论 */
    private Map<String, Object> commentVo(Comment c, User replyTo, Map<Long, User> users) {
        User u = users.get(c.getUserId());
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("commentId", String.valueOf(c.getId()));
        m.put("text", c.getText());
        m.put("createTime", c.getCreateTime() == null ? "" : c.getCreateTime().format(FMT));
        m.put("parentId", String.valueOf(c.getParentId() == null ? 0 : c.getParentId()));
        m.put("likeCount", c.getLikeCount() == null ? 0 : c.getLikeCount());
        m.put("liked", false);
        m.put("user", u == null ? null : Map.of(
                "userId", String.valueOf(u.getId()),
                "nickname", u.getNickname(),
                "avatar", u.getAvatar()));
        return m;
    }
}
