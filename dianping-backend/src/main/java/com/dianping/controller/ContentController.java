package com.dianping.controller;

import com.dianping.common.RequireRole;
import com.dianping.common.R;
import com.dianping.dto.ContentCreateReq;
import com.dianping.dto.ContentVO;
import com.dianping.service.ContentService;
import com.dianping.service.LikeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/content")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;
    private final LikeService likeService;
    private final com.dianping.service.FollowService followService;

    /** POST /content —— 发布点评，仅点评人 */
    @PostMapping
    @RequireRole({"REVIEWER"})
    public R<Map<String, Object>> create(@Valid @RequestBody ContentCreateReq req) {
        return R.ok(contentService.create(com.dianping.common.UserContext.userId(), req));
    }

    /** GET /content/feed?page=&pageSize=&regionCode= */
    @GetMapping("/feed")
    public R<Map<String, Object>> feed(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int pageSize,
                                       @RequestParam(required = false) String regionCode) {
        Long me = com.dianping.common.UserContext.userId();
        Long meId = me != null ? me : 0L;
        return R.ok(contentService.feed(meId, page, pageSize, regionCode));
    }

    /** GET /content/{id} */
    @GetMapping("/{id}")
    public R<ContentVO> detail(@PathVariable Long id) {
        Long me = com.dianping.common.UserContext.userId();
        Long meId = me != null ? me : 0L;
        return R.ok(contentService.detail(meId, id));
    }

    /** GET /content/follow-feed —— 关注 tab：我关注的人的笔记 */
    @GetMapping("/follow-feed")
    public R<Map<String, Object>> followFeed(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int pageSize) {
        Long me = com.dianping.common.UserContext.userId();
        return R.ok(followService.followFeed(me, page, pageSize));
    }

    /** POST /comments/{commentId}/like —— 评论点赞（幂等：重复返回 2003） */
    @PostMapping("/comments/{commentId}/like")
    public R<Void> likeComment(@PathVariable Long commentId) {
        contentService.likeComment(commentId, com.dianping.common.UserContext.userId());
        return R.ok();
    }

    /** DELETE /comments/{commentId}/like —— 取消评论点赞 */
    @DeleteMapping("/comments/{commentId}/like")
    public R<Void> unlikeComment(@PathVariable Long commentId) {
        contentService.unlikeComment(commentId, com.dianping.common.UserContext.userId());
        return R.ok();
    }

    /** GET /content/search?keyword= —— 标题/店铺名模糊搜索（路由优先于 /{id}） */
    @GetMapping("/search")
    public R<Map<String, Object>> search(@RequestParam String keyword,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int pageSize) {
        Long me = com.dianping.common.UserContext.userId();
        Long meId = me != null ? me : 0L;
        return R.ok(contentService.search(meId, keyword.trim(), page, pageSize));
    }

    /** POST /content/{id}/view —— 浏览计数上报 */
    @PostMapping("/{id}/view")
    public R<Void> view(@PathVariable Long id) {
        contentService.addView(id);
        return R.ok();
    }

    /** GET /content/{id}/comments?page= —— 评论列表 */
    @GetMapping("/{id}/comments")
    public R<Map<String, Object>> comments(@PathVariable Long id,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(contentService.commentsOf(id, page, pageSize));
    }

    /** POST /content/{id}/comments  body: {"text":"..."} —— 发布评论 */
    @PostMapping("/{id}/comments")
    public R<Map<String, Object>> comment(@PathVariable Long id,
                                          @RequestBody com.dianping.dto.CommentReq req) {
        return R.ok(contentService.addComment(id, com.dianping.common.UserContext.userId(), req.text(), req.parentId()));
    }

    /** GET /content/{id}/favorite —— 收藏状态：{favorited, favoriteCount} */
    @GetMapping("/{id}/favorite")
    public R<Map<String, Object>> favoriteStatus(@PathVariable Long id) {
        Long me = com.dianping.common.UserContext.userId();
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("favorited", contentService.isFavorited(id, me));
        data.put("favoriteCount", contentService.favoriteCount(id));
        return R.ok(data);
    }

    /** POST /content/{id}/favorite —— 收藏（幂等：重复返回 2003） */
    @PostMapping("/{id}/favorite")
    public R<Void> favorite(@PathVariable Long id) {
        contentService.addFavorite(id, com.dianping.common.UserContext.userId());
        return R.ok();
    }

    /** DELETE /content/{id}/favorite —— 取消收藏 */
    @DeleteMapping("/{id}/favorite")
    public R<Void> unfavorite(@PathVariable Long id) {
        contentService.removeFavorite(id, com.dianping.common.UserContext.userId());
        return R.ok();
    }

    /** DELETE /content/{id} —— 删除笔记（本人或管理员，逻辑删除） */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        Long me = com.dianping.common.UserContext.userId();
        boolean admin = "ADMIN".equals(com.dianping.common.UserContext.role());
        contentService.deleteContent(id, me, admin);
        return R.ok();
    }

    /** DELETE /content/{id}/comments/{commentId} —— 删除评论（本人或管理员） */
    @DeleteMapping("/{id}/comments/{commentId}")
    public R<Void> deleteComment(@PathVariable Long id, @PathVariable Long commentId) {
        Long me = com.dianping.common.UserContext.userId();
        boolean admin = "ADMIN".equals(com.dianping.common.UserContext.role());
        contentService.deleteComment(commentId, me, admin);
        return R.ok();
    }

    /** POST /content/{id}/like —— 重复点赞返回 2003 */
    @PostMapping("/{id}/like")
    public R<Void> like(@PathVariable Long id) {
        likeService.like(com.dianping.common.UserContext.userId(), id);
        return R.ok();
    }

    /** DELETE /content/{id}/like */
    @DeleteMapping("/{id}/like")
    public R<Void> unlike(@PathVariable Long id) {
        likeService.unlike(com.dianping.common.UserContext.userId(), id);
        return R.ok();
    }
}
