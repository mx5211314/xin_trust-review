package com.dianping.module.user;

import com.dianping.common.BizException;
import com.dianping.common.R;
import com.dianping.common.ResultCode;
import com.dianping.module.user.User;
import com.dianping.module.user.UserMapper;
import com.dianping.module.content.ContentService;
import com.dianping.module.oss.OssService;
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
    private final com.dianping.module.interaction.FollowService followService;
    private final com.dianping.module.notify.NotifyService notifyService;
    private final com.dianping.module.interaction.FavoriteService favoriteService;

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

    /** 手机号脱敏：138****0000 */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
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
    public R<Map<String, Object>> notify(@RequestParam(required = false) String category,
                                         @RequestParam(required = false) String subType,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(notifyService.list(currentUserId(), category, subType, page, pageSize));
    }

    /** GET /user/notify/summary —— 分类未读汇总（赞和收藏/新增关注/评论和@） */
    @GetMapping("/notify/summary")
    public R<Map<String, Object>> notifySummary() {
        return R.ok(notifyService.summary(currentUserId()));
    }

    /** GET /user/notify/unread —— 未读数 */
    @GetMapping("/notify/unread")
    public R<java.util.Map<String, Object>> notifyUnread() {
        return R.ok(java.util.Collections.singletonMap("unread", notifyService.unreadCount(currentUserId())));
    }

    /** POST /user/notify/read-all?category= —— 全部已读（可按 interact/announce 分类） */
    @PostMapping("/notify/read-all")
    public R<Void> notifyReadAll(@RequestParam(required = false) String category) {
        notifyService.readAll(currentUserId(), category);
        return R.ok();
    }

    /** POST /user/notify/{notifyId}/read —— 单条已读 */
    @PostMapping("/notify/{notifyId}/read")
    public R<Void> notifyReadOne(@PathVariable Long notifyId) {
        notifyService.markRead(currentUserId(), notifyId);
        return R.ok();
    }

    /** PUT /user/profile —— 编辑资料（昵称/头像/简介） */
    @org.springframework.web.bind.annotation.PutMapping("/profile")
    public R<Void> updateProfile(@org.springframework.web.bind.annotation.RequestBody @jakarta.validation.Valid com.dianping.module.user.UpdateProfileReq req) {
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

    /** GET /user/stats —— 我的数据：关注/粉丝/笔记/获赞/浏览量 */
    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        Long me = currentUserId();
        Map<String, Object> data = contentService.userStats(me);
        data.put("following", followService.followingIds(me).size());
        data.put("followers", followService.followersCount(me));
        return R.ok(data);
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
        return R.ok(favoriteService.favoriteContents(me, page, pageSize));
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
        profile.put("phoneMasked", maskPhone(u.getPhone()));
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
