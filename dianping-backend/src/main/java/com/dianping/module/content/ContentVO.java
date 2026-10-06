package com.dianping.module.content;

import java.util.List;

/**
 * 内容 VO：信息流条目与详情共用。详情比列表多 text/status/rejectReason/liked/favorited/tags。
 * 所有 URL 均为完整可访问地址，客户端不做拼接（契约约定）。
 */
public record ContentVO(
        String contentId,
        String title,
        String text,
        String status,
        String rejectReason,
        String coverUrl,
        List<String> images,
        List<String> tags,
        String videoUrl,
        Integer duration,
        /** 关联门店 id；为空表示这篇笔记没有关联门店（前端据此决定店名是否可点击） */
        String poiId,
        String poiName,
        String regionCode,
        Integer likeCount,
        Integer favoriteCount,
        Integer commentCount,
        Integer viewCount,
        Boolean liked,
        Boolean favorited,
        String createTime,
        Author author,
        /** 浏览时间（仅 /user/browse 列表填充，正常流为空串） */
        String viewTime) {

    public record Author(String userId, String nickname, String avatar, String role) {
    }
}
