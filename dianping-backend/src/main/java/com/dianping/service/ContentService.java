package com.dianping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianping.common.BizException;
import com.dianping.common.ResultCode;
import com.dianping.dto.ContentCreateReq;
import com.dianping.dto.ContentVO;
import com.dianping.entity.Comment;
import com.dianping.entity.Content;
import com.dianping.entity.User;
import com.dianping.entity.UserAction;
import com.dianping.mapper.CommentMapper;
import com.dianping.mapper.ContentMapper;
import com.dianping.mapper.UserActionMapper;
import com.dianping.mapper.UserMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 内容模块：发布（仅点评人）、信息流、详情。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final ContentMapper contentMapper;
    private final UserMapper userMapper;
    private final UserActionMapper userActionMapper;
    private final CommentMapper commentMapper;
    private final com.dianping.mapper.CommentLikeMapper commentLikeMapper;
    private final AuditService auditService;
    private final OssService ossService;
    private final org.springframework.data.redis.core.StringRedisTemplate redis;
    private final NotifyService notifyService;

    /** 发布点评（契约：仅 REVIEWER/ADMIN，权限已由拦截器注解保证） */
    public Map<String, Object> create(Long userId, ContentCreateReq req) {
        boolean isVideo = req.videoKey() != null && !req.videoKey().isBlank();
        if (!isVideo && (req.images() == null || req.images().isEmpty())) {
            throw new BizException(ResultCode.PARAM_ERROR, "图文内容至少要有一张图片");
        }

        // 发布限流：同一用户 60 秒内只能发 1 条（防手抖重复+防刷屏）
        Content latest = contentMapper.selectOne(new LambdaQueryWrapper<Content>()
                .eq(Content::getUserId, userId)
                .orderByDesc(Content::getCreateTime)
                .last("LIMIT 1"));
        if (latest != null && latest.getCreateTime() != null
                && latest.getCreateTime().isAfter(java.time.LocalDateTime.now().minusSeconds(60))) {
            throw new BizException(ResultCode.PARAM_ERROR, "发布太频繁，休息一下再发～");
        }

        Content c = new Content();
        c.setUserId(userId);
        c.setTitle(req.title());
        c.setText(req.text());
        c.setImages(toJson(req.images() == null ? List.of() : req.images()));
        c.setTags(toJson(req.tags() == null ? List.of() : req.tags()));
        c.setVideoKey(isVideo ? req.videoKey() : "");
        c.setCoverKey(isVideo ? nullToEmpty(req.coverKey()) : "");
        c.setDuration(isVideo && req.duration() != null ? req.duration() : 0);
        c.setRegionCode(req.regionCode());
        c.setPoiName(nullToEmpty(req.poiName()));
        c.setStatus("PENDING");
        c.setRejectReason("");
        c.setLikeCount(0);
        c.setViewCount(0);
        contentMapper.insert(c);

        // 机审：通过 -> 上架；不通过 -> 停 PENDING 转后台人工
        boolean pass = auditService.machinePass(c);
        if (pass) {
            c.setStatus("APPROVED");
            c.setAuditTime(java.time.LocalDateTime.now());
            contentMapper.updateById(c);
        }
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("contentId", String.valueOf(c.getId()));
        data.put("status", c.getStatus());
        if (!pass) {
            data.put("tip", "审核中");
        }
        return data;
    }

    /** 信息流：仅 APPROVED，按地区可选过滤，时间倒序 */
    public Map<String, Object> feed(Long me, int page, int pageSize, String regionCode) {
        Page<Content> p = contentMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<Content>()
                        .eq(Content::getStatus, "APPROVED")
                        .eq(regionCode != null && !regionCode.isBlank(), Content::getRegionCode, regionCode)
                        .orderByDesc(Content::getCreateTime));
        return pageResult(me, p);
    }

    /** 用户主页：TA 发布的、仅 APPROVED 的内容 */
    public Map<String, Object> userContents(Long me, Long targetUserId, int page, int pageSize) {
        Page<Content> p = contentMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<Content>()
                        .eq(Content::getUserId, targetUserId)
                        .eq(Content::getStatus, "APPROVED")
                        .orderByDesc(Content::getCreateTime));
        return pageResult(me, p);
    }

    /** 详情：作者本人可见自己的 REJECTED（附原因），其他人只可见 APPROVED */
    public ContentVO detail(Long me, Long id) {
        Content c = contentMapper.selectById(id);
        if (c == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        boolean visible = "APPROVED".equals(c.getStatus())
                || ("REJECTED".equals(c.getStatus()) && c.getUserId().equals(me));
        if (!visible) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return toVoDetail(c, me);
    }

    /** 评论列表（时间倒序；带回复关系，前端按 parentId 缩进） */
    public Map<String, Object> commentsOf(Long contentId, int page, int pageSize) {
        Page<Comment> p = commentMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getContentId, contentId)
                        .orderByDesc(Comment::getId));
        List<Long> uids = p.getRecords().stream()
                .flatMap(c -> java.util.stream.Stream.of(c.getUserId(), c.getReplyToUserId()))
                .filter(id -> id != null && id > 0)
                .distinct().toList();
        Map<Long, User> users = uids.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(uids).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));
        List<Map<String, Object>> list = p.getRecords().stream().map(c -> {
            User u = users.get(c.getUserId());
            User replyTo = c.getReplyToUserId() != null && c.getReplyToUserId() > 0
                    ? users.get(c.getReplyToUserId()) : null;
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("commentId", String.valueOf(c.getId()));
            m.put("text", c.getText());
            m.put("createTime", c.getCreateTime() == null ? "" : c.getCreateTime().format(FMT));
            m.put("parentId", String.valueOf(c.getParentId() == null ? 0 : c.getParentId()));
            m.put("replyToNickname", replyTo == null ? "" : replyTo.getNickname());
            m.put("user", u == null ? null : Map.of(
                    "userId", String.valueOf(u.getId()),
                    "nickname", u.getNickname(),
                    "avatar", u.getAvatar()));
            return m;
        }).toList();
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("total", p.getTotal());
        data.put("list", list);
        return data;
    }

    /** 发布评论（支持回复：传 parentId 时自动带上被回复人） */
    public Map<String, Object> addComment(Long contentId, Long me, String text, Long parentId) {
        if (contentMapper.selectById(contentId) == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
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
            c.setParentId(parentId);
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
                new LambdaQueryWrapper<com.dianping.entity.CommentLike>()
                        .eq(com.dianping.entity.CommentLike::getCommentId, commentId)
                        .eq(com.dianping.entity.CommentLike::getUserId, me)) > 0;
        if (exists) {
            throw new BizException(ResultCode.DUPLICATE_ACTION);
        }
        com.dianping.entity.CommentLike cl = new com.dianping.entity.CommentLike();
        cl.setCommentId(commentId);
        cl.setUserId(me);
        commentLikeMapper.insert(cl);
        c.setLikeCount((c.getLikeCount() == null ? 0 : c.getLikeCount()) + 1);
        commentMapper.updateById(c);
    }

    /** 取消评论点赞 */
    public void unlikeComment(Long commentId, Long me) {
        int deleted = commentLikeMapper.delete(
                new LambdaQueryWrapper<com.dianping.entity.CommentLike>()
                        .eq(com.dianping.entity.CommentLike::getCommentId, commentId)
                        .eq(com.dianping.entity.CommentLike::getUserId, me));
        if (deleted > 0) {
            Comment c = commentMapper.selectById(commentId);
            if (c != null) {
                c.setLikeCount(Math.max(0, (c.getLikeCount() == null ? 0 : c.getLikeCount()) - 1));
                commentMapper.updateById(c);
            }
        }
    }

    /** 收藏数 */
    public long favoriteCount(Long contentId) {
        return userActionMapper.selectCount(new LambdaQueryWrapper<UserAction>()
                .eq(UserAction::getContentId, contentId)
                .eq(UserAction::getType, UserAction.TYPE_FAV));
    }

    /** 是否已收藏 */
    public boolean isFavorited(Long contentId, Long me) {
        if (me == null) return false;
        return userActionMapper.selectCount(new LambdaQueryWrapper<UserAction>()
                .eq(UserAction::getUserId, me)
                .eq(UserAction::getContentId, contentId)
                .eq(UserAction::getType, UserAction.TYPE_FAV)) > 0;
    }

    /** 收藏（幂等：重复收藏返回 2003） */
    public void addFavorite(Long contentId, Long me) {
        if (contentMapper.selectById(contentId) == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        if (isFavorited(contentId, me)) {
            throw new BizException(ResultCode.DUPLICATE_ACTION);
        }
        UserAction a = new UserAction();
        a.setUserId(me);
        a.setContentId(contentId);
        a.setType(UserAction.TYPE_FAV);
        userActionMapper.insert(a);
    }

    /** 取消收藏 */
    public void removeFavorite(Long contentId, Long me) {
        userActionMapper.delete(new LambdaQueryWrapper<UserAction>()
                .eq(UserAction::getUserId, me)
                .eq(UserAction::getContentId, contentId)
                .eq(UserAction::getType, UserAction.TYPE_FAV));
    }

    /** 我的收藏列表（按收藏时间倒序） */
    public Map<String, Object> favoriteContents(Long me, int page, int pageSize) {
        Page<UserAction> ap = userActionMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<UserAction>()
                        .eq(UserAction::getUserId, me)
                        .eq(UserAction::getType, UserAction.TYPE_FAV)
                        .orderByDesc(UserAction::getId));
        List<Long> ids = ap.getRecords().stream().map(UserAction::getContentId).toList();
        Map<String, Object> data = new java.util.HashMap<>();
        if (ids.isEmpty()) {
            data.put("total", 0);
            data.put("list", java.util.List.of());
            return data;
        }
        Map<Long, Content> byId = contentMapper.selectBatchIds(ids).stream()
                .filter(c -> "APPROVED".equals(c.getStatus()))
                .collect(Collectors.toMap(Content::getId, c -> c));
        List<Content> ordered = ids.stream().map(byId::get)
                .filter(java.util.Objects::nonNull).toList();
        List<Long> userIds = ordered.stream().map(Content::getUserId).distinct().toList();
        Map<Long, User> users = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));
        List<ContentVO> list = ordered.stream()
                .map(c -> toVo(c, users.get(c.getUserId()), true, false, false, 0, 0))
                .toList();
        data.put("total", ap.getTotal());
        data.put("list", list);
        return data;
    }

    /** 后台详情：任意状态可见，不做可见性校验 */
    public ContentVO adminDetail(Long id) {
        Content c = contentMapper.selectById(id);
        if (c == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return toVoDetail(c, null);
    }

    /** 后台内容列表：返回 VO（含完整图片/封面 URL），任意状态 */
    public Map<String, Object> adminVoPage(String status, int page, int pageSize) {
        Page<Content> p = contentMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<Content>()
                        .eq(status != null && !status.isBlank(), Content::getStatus, status)
                        .orderByDesc(Content::getCreateTime));
        List<ContentVO> vos = p.getRecords().stream()
                .map(c -> toVo(c, null, false, true, false, 0, 0))
                .toList();
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("total", p.getTotal());
        data.put("list", vos);
        return data;
    }

    /** 搜索：标题 / 店铺名 / 话题标签 模糊匹配（仅上架内容） */
    public Map<String, Object> search(Long me, String keyword, int page, int pageSize) {
        Page<Content> p = contentMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<Content>()
                        .eq(Content::getStatus, "APPROVED")
                        .and(w -> w.like(Content::getTitle, keyword)
                                .or().like(Content::getPoiName, keyword)
                                .or().like(Content::getTags, keyword))
                        .orderByDesc(Content::getCreateTime));
        return pageResult(me, p);
    }

    /** 删除自己的笔记（管理员可删任意）：逻辑删除 */
    public void deleteContent(Long id, Long me, boolean isAdmin) {
        Content c = contentMapper.selectById(id);
        if (c == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        if (!isAdmin && !c.getUserId().equals(me)) {
            throw new BizException(ResultCode.FORBIDDEN, "只能删除自己的笔记");
        }
        contentMapper.deleteById(id);

        // 删除联动：清理评论、点赞/收藏行为、Redis 点赞计数
        commentMapper.delete(new LambdaQueryWrapper<Comment>().eq(Comment::getContentId, id));
        userActionMapper.delete(new LambdaQueryWrapper<UserAction>().eq(UserAction::getContentId, id));
        redis.opsForHash().delete("like:cnt", String.valueOf(id));
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

    /** 指定用户集合的笔记流（关注 tab 用，时间倒序） */
    public Map<String, Object> contentsOfUsers(Long me, List<Long> userIds, int page, int pageSize) {
        Page<Content> p = contentMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<Content>()
                        .eq(Content::getStatus, "APPROVED")
                        .in(Content::getUserId, userIds)
                        .orderByDesc(Content::getCreateTime));
        return pageResult(me, p);
    }

    /** 浏览计数 +1（详情页打开时上报） */
    public void addView(Long id) {
        contentMapper.incrView(id);
    }

    /** 我的数据三卡：发布数 / 获赞 / 浏览量 */
    public Map<String, Object> userStats(Long userId) {
        Map<String, Object> raw = contentMapper.statsOfUser(userId);
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("posts", ((Number) raw.getOrDefault("posts", 0)).longValue());
        data.put("likes", ((Number) raw.getOrDefault("likes", 0)).longValue());
        data.put("views", ((Number) raw.getOrDefault("views", 0)).longValue());
        return data;
    }

    /** 我赞过的内容（"赞过" tab，按点赞时间倒序） */
    public Map<String, Object> likedContents(Long me, int page, int pageSize) {
        Page<UserAction> ap = userActionMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<UserAction>()
                        .eq(UserAction::getUserId, me)
                        .eq(UserAction::getType, UserAction.TYPE_LIKE)
                        .orderByDesc(UserAction::getId));
        List<Long> ids = ap.getRecords().stream().map(UserAction::getContentId).toList();
        Map<String, Object> data = new java.util.HashMap<>();
        if (ids.isEmpty()) {
            data.put("total", 0);
            data.put("list", java.util.List.of());
            return data;
        }
        Map<Long, Content> byId = contentMapper.selectBatchIds(ids).stream()
                .filter(c -> "APPROVED".equals(c.getStatus()))
                .collect(Collectors.toMap(Content::getId, c -> c));
        List<Content> ordered = ids.stream().map(byId::get)
                .filter(java.util.Objects::nonNull).toList();
        List<Long> userIds = ordered.stream().map(Content::getUserId).distinct().toList();
        Map<Long, User> users = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));
        List<ContentVO> list = ordered.stream()
                .map(c -> toVo(c, users.get(c.getUserId()), true, false, false, 0, 0))
                .toList();
        data.put("total", ap.getTotal());
        data.put("list", list);
        return data;
    }

    // ---------- 内部 ----------

    private Map<String, Object> pageResult(Long me, Page<Content> p) {
        List<Content> records = p.getRecords();
        Set<Long> likedSet = likedSet(me, records);

        List<Long> userIds = records.stream().map(Content::getUserId).distinct().toList();
        Map<Long, User> users = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));

        List<ContentVO> list = records.stream()
                .map(c -> toVo(c, users.get(c.getUserId()), likedSet.contains(c.getId()), false, false, 0, 0))
                .toList();

        Map<String, Object> data = new java.util.HashMap<>();
        data.put("total", p.getTotal());
        data.put("list", list);
        return data;
    }

    /** 带作者信息的 VO 转换（列表/详情共用） */
    private ContentVO toVo(Content c, User author, boolean liked, boolean withDetail,
                           boolean favorited, long favoriteCount, long commentCount) {
        List<String> imageKeys = fromJson(c.getImages());
        List<String> imageUrls = imageKeys.stream().map(ossService::publicUrl).toList();
        ContentVO.Author a = author == null ? null
                : new ContentVO.Author(String.valueOf(author.getId()), author.getNickname(), author.getAvatar());
        return new ContentVO(
                String.valueOf(c.getId()),
                c.getTitle(),
                withDetail ? c.getText() : "",
                c.getStatus(),
                withDetail ? c.getRejectReason() : "",
                ossService.publicUrl(c.getCoverKey()),
                imageUrls,
                fromJson(c.getTags()),
                c.getVideoKey() == null || c.getVideoKey().isBlank() ? "" : ossService.publicUrl(c.getVideoKey()),
                c.getDuration(),
                c.getPoiName(),
                c.getRegionCode(),
                c.getLikeCount(),
                (int) favoriteCount,
                (int) commentCount,
                c.getViewCount(),
                liked,
                favorited,
                c.getCreateTime() == null ? "" : c.getCreateTime().format(FMT),
                a);
    }

    /** 单条转换：自己查作者 + 查"我是否点过赞/收藏过" + 评论/收藏计数（详情专用） */
    private ContentVO toVoDetail(Content c, Long me) {
        User author = c.getUserId() == null ? null : userMapper.selectById(c.getUserId());
        Set<Long> likedSet = likedSet(me, List.of(c));
        boolean favorited = me != null && isFavorited(c.getId(), me);
        long favCount = favoriteCount(c.getId());
        long cmtCount = commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getContentId, c.getId()));
        return toVo(c, author, likedSet.contains(c.getId()), true, favorited, favCount, cmtCount);
    }

    /** 批量查"我是否点过赞"（user_action 唯一索引，快） */
    private Set<Long> likedSet(Long me, List<Content> contents) {
        if (me == null || contents.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> ids = contents.stream().map(Content::getId).toList();
        return userActionMapper.selectList(new LambdaQueryWrapper<UserAction>()
                        .eq(UserAction::getUserId, me)
                        .eq(UserAction::getType, UserAction.TYPE_LIKE)
                        .in(UserAction::getContentId, ids))
                .stream().map(UserAction::getContentId).collect(Collectors.toSet());
    }

    private String toJson(List<String> list) {
        try {
            return JSON.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return JSON.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
