package com.dianping.controller;

import com.dianping.common.BizException;
import com.dianping.common.R;
import com.dianping.common.ResultCode;
import com.dianping.entity.User;
import com.dianping.mapper.UserMapper;
import com.dianping.service.ContentService;
import com.dianping.service.OssService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;
    private final ContentService contentService;
    private final OssService ossService;
    private final com.dianping.service.FollowService followService;
    private final com.dianping.service.NotifyService notifyService;

    /** GET /user/me */
    @GetMapping("/me")
    public R<Map<String, Object>> me() {
        Long me = currentUserId();
        User u = userMapper.selectById(me);
        if (u == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("userId", String.valueOf(u.getId()));
        data.put("nickname", u.getNickname());
        data.put("avatar", u.getAvatar());
        data.put("role", u.getRole());
        data.put("phone", u.getPhone());
        return R.ok(data);
    }

    /** POST /user/{userId}/follow —— 关注（幂等：重复返回 2003） */
    @org.springframework.web.bind.annotation.PostMapping("/{userId}/follow")
    public R<Void> follow(@PathVariable Long userId) {
        followService.follow(currentUserId(), userId);
        return R.ok();
    }

    /** DELETE /user/{userId}/follow —— 取关 */
    @org.springframework.web.bind.annotation.DeleteMapping("/{userId}/follow")
    public R<Void> unfollow(@PathVariable Long userId) {
        followService.unfollow(currentUserId(), userId);
        return R.ok();
    }

    /** GET /user/following?page= —— 我关注的列表 */
    @GetMapping("/following")
    public R<Map<String, Object>> following(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(followService.followingList(currentUserId(), page, pageSize));
    }

    /** GET /user/notify —— 消息列表 */
    @GetMapping("/notify")
    public R<Map<String, Object>> notify(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(notifyService.list(currentUserId(), page, pageSize));
    }

    /** GET /user/notify/unread —— 未读数 */
    @GetMapping("/notify/unread")
    public R<java.util.Map<String, Object>> notifyUnread() {
        return R.ok(java.util.Collections.singletonMap("unread", notifyService.unreadCount(currentUserId())));
    }

    /** POST /user/notify/read-all —— 全部已读 */
    @PostMapping("/notify/read-all")
    public R<Void> notifyReadAll() {
        notifyService.readAll(currentUserId());
        return R.ok();
    }

    /** PUT /user/profile —— 编辑资料（昵称/头像/简介） */
    @org.springframework.web.bind.annotation.PutMapping("/profile")
    public R<Void> updateProfile(@org.springframework.web.bind.annotation.RequestBody @jakarta.validation.Valid com.dianping.dto.UpdateProfileReq req) {
        Long me = currentUserId();
        User u = userMapper.selectById(me);
        if (u == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        u.setNickname(req.nickname().trim());
        if (req.avatar() != null && !req.avatar().isBlank()) {
            // 相对 key 转完整 URL（local 模式跟随当前 Host）；已是 http 直接过
            u.setAvatar(ossService.publicUrl(req.avatar()));
        }
        if (req.bio() != null) {
            u.setBio(req.bio().trim());
        }
        userMapper.updateById(u);
        return R.ok();
    }

    /** GET /user/stats —— 我的数据三卡：发布/获赞/浏览量 */
    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        Long me = currentUserId();
        return R.ok(contentService.userStats(me));
    }

    /** GET /user/liked?page=&pageSize= —— 我赞过的内容（"赞过" tab） */
    @GetMapping("/liked")
    public R<Map<String, Object>> liked(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int pageSize) {
        Long me = currentUserId();
        return R.ok(contentService.likedContents(me, page, pageSize));
    }

    /** GET /user/favorites?page=&pageSize= —— 我的收藏 */
    @GetMapping("/favorites")
    public R<Map<String, Object>> favorites(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int pageSize) {
        Long me = currentUserId();
        return R.ok(contentService.favoriteContents(me, page, pageSize));
    }

    /** GET /user/{userId}?page=&pageSize= —— 用户主页 + TA 的点评列表 */
    @GetMapping("/{userId}")
    public R<Map<String, Object>> profile(@PathVariable Long userId,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int pageSize) {
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        Map<String, Object> profile = new HashMap<>();
        profile.put("userId", String.valueOf(u.getId()));
        profile.put("nickname", u.getNickname());
        profile.put("avatar", u.getAvatar());
        profile.put("role", u.getRole());
        profile.put("status", u.getStatus());
        profile.put("followers", followService.followersCount(u.getId()));
        Long meId0 = currentUserId();
        profile.put("following", followService.isFollowing(meId0, u.getId()));
        Long me = currentUserId();
        Long meId = me != null ? me : 0L;
        return R.ok(Map.of(
                "profile", profile,
                "contents", contentService.userContents(meId, userId, page, pageSize)));
    }

    private Long currentUserId() {
        Long id = com.dianping.common.UserContext.userId();
        if (id == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return id;
    }
}
