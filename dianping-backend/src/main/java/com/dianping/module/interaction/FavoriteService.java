package com.dianping.module.interaction;

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
