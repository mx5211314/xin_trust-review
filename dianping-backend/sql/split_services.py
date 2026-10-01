# -*- coding: utf-8 -*-
"""把 ContentService 的评论/收藏相关方法切到 CommentService / FavoriteService"""
import io, re

BASE = r"D:\桌面文件\111\dianping-backend\src\main\java\com\dianping"
CS = BASE + r"\module\content\ContentService.java"

src = io.open(CS, encoding='utf-8').read()


def cut(src, marker):
    """按签名关键字定位方法（含前置注释），括号计数切出，返回 (方法文本, 剩余源码)"""
    idx = src.find(marker)
    if idx < 0:
        raise SystemExit('marker not found: ' + marker)
    # 回溯到行首，并把紧邻上方的 /** ... */ 注释一起带走
    line_start = src.rfind('\n', 0, idx) + 1
    look = src.rfind('}', 0, line_start)
    prev = src.rfind('/**', 0, line_start)
    if prev > look:
        # 注释与当前方法之间不能有别的代码
        between = src[prev:line_start].strip()
        if between.endswith('*/'):
            line_start = prev
    # 从 marker 处找方法体的 { 并计数到闭合
    i = src.find('{', idx)
    depth = 0
    j = i
    while j < len(src):
        if src[j] == '{':
            depth += 1
        elif src[j] == '}':
            depth -= 1
            if depth == 0:
                break
        j += 1
    body = src[line_start:j + 1]
    rest = src[:line_start] + src[j + 1:]
    return body.strip('\n'), rest


COMMENT_METHODS = [
    'public Map<String, Object> commentsOf',
    'public Map<String, Object> addComment',
    'public void likeComment',
    'public void unlikeComment',
    'public void deleteComment',
    'private Map<String, Object> commentVo',
]
FAVORITE_METHODS = [
    'public long favoriteCount',
    'public boolean isFavorited',
    'public void addFavorite',
    'public void removeFavorite',
    'public Map<String, Object> favoriteContents',
]
# 收藏方法从 ContentService 中移除后由 FavoriteService 手工重写（依赖不同）

comment_bodies, fav_bodies = [], []
for m in COMMENT_METHODS:
    b, src = cut(src, m)
    comment_bodies.append(b)
for m in FAVORITE_METHODS:
    _, src = cut(src, m)   # 丢弃，FavoriteService 手工重写

io.open(CS, 'w', encoding='utf-8').write(src)
print('ContentService trimmed')

# ---------------- CommentService ----------------
comment_cls = '''package com.dianping.module.comment;

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

''' + '\n\n'.join('    ' + b.replace('\n', '\n    ') if False else b for b in comment_bodies) + '''
}
'''
io.open(BASE + r'\module\comment\CommentService.java', 'w', encoding='utf-8').write(comment_cls)
print('CommentService written')

# ---------------- FavoriteService ----------------
fav_cls = '''package com.dianping.module.interaction;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianping.common.BizException;
import com.dianping.common.ResultCode;
import com.dianping.module.content.ContentMapper;
import com.dianping.module.content.ContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 收藏域：收藏 / 取消 / 状态 / 收藏列表（user_action type=2）。
 */
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final UserActionMapper userActionMapper;
    private final ContentMapper contentMapper;
    private final ContentService contentService;

    /** 收藏数 */
    public long favoriteCount(Long contentId) {
        return userActionMapper.selectCount(new LambdaQueryWrapper<UserAction>()
                .eq(UserAction::getContentId, contentId)
                .eq(UserAction::getType, UserAction.TYPE_FAV));
    }

    /** 是否已收藏 */
    public boolean isFavorited(Long contentId, Long me) {
        if (me == null) {
            return false;
        }
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
        try {
            userActionMapper.insert(a);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new BizException(ResultCode.DUPLICATE_ACTION);
        }
    }

    /** 取消收藏 */
    public void removeFavorite(Long contentId, Long me) {
        userActionMapper.delete(new LambdaQueryWrapper<UserAction>()
                .eq(UserAction::getUserId, me)
                .eq(UserAction::getContentId, contentId)
                .eq(UserAction::getType, UserAction.TYPE_FAV));
    }

    /** 我的收藏列表（按收藏时间倒序；VO 组装复用内容域） */
    public Map<String, Object> favoriteContents(Long me, int page, int pageSize) {
        Page<UserAction> ap = userActionMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<UserAction>()
                        .eq(UserAction::getUserId, me)
                        .eq(UserAction::getType, UserAction.TYPE_FAV)
                        .orderByDesc(UserAction::getId));
        List<Long> ids = ap.getRecords().stream().map(UserAction::getContentId).toList();
        Map<String, Object> data = new HashMap<>();
        data.put("total", ap.getTotal());
        data.put("list", contentService.voListByIds(me, ids));
        return data;
    }
}
'''
io.open(BASE + r'\module\interaction\FavoriteService.java', 'w', encoding='utf-8').write(fav_cls)
print('FavoriteService written')
