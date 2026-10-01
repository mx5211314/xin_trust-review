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
    private final FavoriteFolderMapper folderMapper;

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

    /** 收藏（幂等：已在收藏夹则视为"移动到该夹"）；folderId 指定收藏夹，0=未分类 */
    public void addFavorite(Long contentId, Long me, long folderId) {
        if (contentMapper.selectById(contentId) == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        // 校验收藏夹归属（folderId=0 为未分类，无需校验）——移动/新增两条路径都要过
        if (folderId != 0) {
            FavoriteFolder f = folderMapper.selectById(folderId);
            if (f == null || !me.equals(f.getUserId())) {
                throw new BizException(ResultCode.NOT_FOUND, "收藏夹不存在");
            }
        }
        UserAction existing = userActionMapper.selectOne(new LambdaQueryWrapper<UserAction>()
                .eq(UserAction::getUserId, me)
                .eq(UserAction::getContentId, contentId)
                .eq(UserAction::getType, UserAction.TYPE_FAV)
                .last("LIMIT 1"));
        if (existing != null) {
            // 已收藏：更新所属收藏夹（实现"移动到其他夹"）
            if (existing.getFolderId() == null || existing.getFolderId() != folderId) {
                existing.setFolderId(folderId);
                userActionMapper.updateById(existing);
            }
            return;
        }
        UserAction a = new UserAction();
        a.setUserId(me);
        a.setContentId(contentId);
        a.setType(UserAction.TYPE_FAV);
        a.setFolderId(folderId);
        userActionMapper.insert(a);
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
        return favoriteContents(me, 0L, page, pageSize);
    }

    /** 指定收藏夹的收藏列表（folderId=0 为未分类） */
    public Map<String, Object> favoriteContents(Long me, long folderId, int page, int pageSize) {
        Page<UserAction> ap = userActionMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<UserAction>()
                        .eq(UserAction::getUserId, me)
                        .eq(UserAction::getType, UserAction.TYPE_FAV)
                        .eq(UserAction::getFolderId, folderId)
                        .orderByDesc(UserAction::getId));
        List<Long> ids = ap.getRecords().stream().map(UserAction::getContentId).toList();
        Map<String, Object> data = new HashMap<>();
        data.put("total", ap.getTotal());
        data.put("list", contentService.voListByIds(me, ids));
        return data;
    }

    /** 新建收藏夹，返回新夹 id */
    public long createFolder(Long me, String name) {
        String n = (name == null ? "" : name.trim());
        if (n.isEmpty()) n = "我的收藏夹";
        if (n.length() > 20) n = n.substring(0, 20);
        FavoriteFolder f = new FavoriteFolder();
        f.setUserId(me);
        f.setName(n);
        f.setSort(0);
        f.setCreateTime(java.time.LocalDateTime.now());
        folderMapper.insert(f);
        return f.getId();
    }

    /** 我的收藏夹列表（含每个夹的收藏数 + 未分类聚合） */
    public List<Map<String, Object>> listFolders(Long me) {
        long uncategorized = userActionMapper.selectCount(new LambdaQueryWrapper<UserAction>()
                .eq(UserAction::getUserId, me)
                .eq(UserAction::getType, UserAction.TYPE_FAV)
                .eq(UserAction::getFolderId, 0));
        List<FavoriteFolder> folders = folderMapper.selectList(
                new LambdaQueryWrapper<FavoriteFolder>()
                        .eq(FavoriteFolder::getUserId, me)
                        .orderByAsc(FavoriteFolder::getSort)
                        .orderByDesc(FavoriteFolder::getCreateTime));
        List<Map<String, Object>> list = new java.util.ArrayList<>();
        Map<String, Object> m0 = new java.util.HashMap<>();
        m0.put("folderId", 0);
        m0.put("name", "未分类");
        m0.put("count", uncategorized);
        list.add(m0);
        for (FavoriteFolder f : folders) {
            long c = userActionMapper.selectCount(new LambdaQueryWrapper<UserAction>()
                    .eq(UserAction::getUserId, me)
                    .eq(UserAction::getType, UserAction.TYPE_FAV)
                    .eq(UserAction::getFolderId, f.getId()));
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("folderId", f.getId());
            m.put("name", f.getName());
            m.put("count", c);
            list.add(m);
        }
        return list;
    }
}
