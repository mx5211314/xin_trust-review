package com.dianping.module.content;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianping.common.BizException;
import com.dianping.common.ResultCode;
import com.dianping.module.content.ContentCreateReq;
import com.dianping.module.content.ContentVO;
import com.dianping.module.content.Content;
import com.dianping.module.user.User;
import com.dianping.module.content.ContentMapper;
import com.dianping.module.user.UserMapper;
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
import com.dianping.module.notify.NotifyService;
import com.dianping.module.oss.OssService;
import com.dianping.module.comment.CommentService;
import com.dianping.module.interaction.FavoriteService;
import com.dianping.module.interaction.UserAction;
import com.dianping.module.interaction.UserActionMapper;

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
    private final CommentService commentService;
    private final com.dianping.module.comment.CommentLikeMapper commentLikeMapper;
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

        // @提及：解析正文里的 @昵称，给被提及的用户发站内通知（零额外字段，复用 notify 表）
        Set<String> nicks = parseMentions(req.text());
        if (!nicks.isEmpty()) {
            List<User> mentioned = userMapper.selectList(
                    new LambdaQueryWrapper<User>().in(User::getNickname, nicks));
            for (User u : mentioned) {
                notifyService.send(u.getId(), NotifyService.T_MENTION, userId, c.getId(), null, null);
            }
        }

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

    /** 解析正文中 @昵称（中文/字母/数字/连字符，遇空格或标点截断） */
    private static Set<String> parseMentions(String text) {
        Set<String> nicks = new java.util.LinkedHashSet<>();
        if (text == null || text.isBlank()) return nicks;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("@([\\u4e00-\\u9fa5A-Za-z0-9_\\-]+)");
        java.util.regex.Matcher m = p.matcher(text);
        while (m.find()) {
            nicks.add(m.group(1));
        }
        return nicks;
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

    /** 热门话题：统计最近已上架内容的 tags 词频（TopN） */
    public List<Map<String, Object>> hotTags(int limit) {
        List<Content> recents = contentMapper.selectList(
                new LambdaQueryWrapper<Content>()
                        .eq(Content::getStatus, "APPROVED")
                        .orderByDesc(Content::getCreateTime)
                        .last("LIMIT 300"));
        java.util.Map<String, Integer> cnt = new java.util.HashMap<>();
        for (Content c : recents) {
            for (String t : fromJson(c.getTags())) {
                if (t != null && !t.isBlank()) cnt.merge(t, 1, Integer::sum);
            }
        }
        return cnt.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(limit)
                .map(e -> {
                    java.util.Map<String, Object> m = new java.util.HashMap<>();
                    m.put("tag", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                }).collect(java.util.stream.Collectors.toList());
    }

    /** 热门店铺：统计最近已上架内容的 poiName 词频（TopN） */
    public List<Map<String, Object>> hotShops(int limit) {
        List<Content> recents = contentMapper.selectList(
                new LambdaQueryWrapper<Content>()
                        .eq(Content::getStatus, "APPROVED")
                        .isNotNull(Content::getPoiName)
                        .ne(Content::getPoiName, "")
                        .orderByDesc(Content::getCreateTime)
                        .last("LIMIT 300"));
        java.util.Map<String, Integer> cnt = new java.util.HashMap<>();
        for (Content c : recents) {
            String p = c.getPoiName();
            if (p != null && !p.isBlank()) cnt.merge(p, 1, Integer::sum);
        }
        return cnt.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(limit)
                .map(e -> {
                    java.util.Map<String, Object> m = new java.util.HashMap<>();
                    m.put("shop", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                }).collect(java.util.stream.Collectors.toList());
    }

    /** 编辑自己的笔记：文字信息可改，修改后重新走机审（状态回 PENDING） */
    public void updateContent(Long id, Long me, ContentCreateReq req) {
        Content c = contentMapper.selectById(id);
        if (c == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        if (!c.getUserId().equals(me)) {
            throw new BizException(ResultCode.FORBIDDEN, "只能编辑自己的笔记");
        }
        c.setTitle(req.title().trim());
        c.setText(req.text().trim());
        c.setRegionCode(req.regionCode());
        c.setPoiName(nullToEmpty(req.poiName()));
        c.setTags(toJson(req.tags() == null ? List.of() : req.tags()));
        // 图片可整组替换（传了才更新）
        if (req.images() != null && !req.images().isEmpty()) {
            c.setImages(toJson(req.images()));
        }
        // 修改后重新审核
        c.setStatus("PENDING");
        c.setRejectReason("");
        contentMapper.updateById(c);
        boolean pass = auditService.machinePass(c);
        if (pass) {
            c.setStatus("APPROVED");
            c.setAuditTime(java.time.LocalDateTime.now());
            contentMapper.updateById(c);
        }
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
        commentService.deleteByContent(id);
        userActionMapper.delete(new LambdaQueryWrapper<UserAction>().eq(UserAction::getContentId, id));
        redis.opsForSet().remove(com.dianping.common.DianpingConst.REDIS_LIKE_DIRTY, String.valueOf(id));
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

    /** 按 id 列表组装 VO（保持传入顺序，只保留已上架；供收藏/赞过等列表复用） */
    public List<ContentVO> voListByIds(Long me, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
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
        return ordered.stream()
                .map(c -> toVo(c, users.get(c.getUserId()), true, false, false, 0, 0))
                .toList();
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
        List<ContentVO> list = voListByIds(me, ids);
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
        // 收藏计数/状态直接查关系表（不依赖收藏服务，避免循环依赖）
        long favCount = userActionMapper.selectCount(new LambdaQueryWrapper<UserAction>()
                .eq(UserAction::getContentId, c.getId())
                .eq(UserAction::getType, UserAction.TYPE_FAV));
        boolean favorited = me != null && userActionMapper.selectCount(new LambdaQueryWrapper<UserAction>()
                .eq(UserAction::getUserId, me)
                .eq(UserAction::getContentId, c.getId())
                .eq(UserAction::getType, UserAction.TYPE_FAV)) > 0;
        long cmtCount = commentService.countOf(c.getId());
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
