package com.dianping.module.user;

import com.dianping.common.BizException;
import com.dianping.common.R;
import com.dianping.common.ResultCode;
import com.dianping.module.user.User;
import com.dianping.module.user.UserMapper;
import com.dianping.module.content.ContentService;
import com.dianping.module.oss.OssService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final com.dianping.module.interaction.BlockService blockService;

    /** GET /user/by-nickname?nick= —— 按昵称查用户（供 @提及 跳转用户主页） */
    @GetMapping("/by-nickname")
    public R<Map<String, Object>> byNickname(@RequestParam String nick) {
        User u = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getNickname, nick).last("LIMIT 1"));
        if (u == null) {
            return R.ok(java.util.Collections.emptyMap());
        }
        Map<String, Object> data = new HashMap<>();
        data.put("userId", String.valueOf(u.getId()));
        data.put("nickname", u.getNickname());
        data.put("avatar", avatarUrl(u.getAvatar()));
        return R.ok(data);
    }

    /** GET /user/search?keyword=&limit= —— 用户搜索（按昵称模糊，供搜索页"用户"Tab） */
    @GetMapping("/search")
    public R<List<Map<String, Object>>> searchUsers(@RequestParam String keyword,
                                                     @RequestParam(defaultValue = "20") int limit) {
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .like(User::getNickname, keyword)
                        .last("LIMIT " + Math.min(limit, 50)));
        List<Map<String, Object>> list = users.stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("userId", String.valueOf(u.getId()));
            m.put("nickname", u.getNickname());
            m.put("avatar", avatarUrl(u.getAvatar()));
            return m;
        }).collect(Collectors.toList());
        return R.ok(list);
    }

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
        data.put("avatar", avatarUrl(u.getAvatar()));
        data.put("role", u.getRole());
        data.put("phone", u.getPhone());
        // bio 必须下发：前端编辑资料用它回填。原来漏了这个字段，
        // 编辑框回填成空、一保存就把用户真实的简介覆盖成空串（静默丢数据）
        data.put("bio", u.getBio());
        return R.ok(data);
    }

    /**
     * 头像对外地址：库里存的是**存储 key**，下发时统一转成可访问 URL。
     *
     * 为什么要这么绕：URL 里带 Host（local 模式跟随请求 Host、OSS 模式带 Bucket 域名），
     * 存进库换域名就失效 —— 本地隧道域名每次重启都会变，头像会集体变白。
     * 和内容图片保持一致：**存 key、读时转 URL**。
     * 历史数据里已经存了完整 URL 的，publicUrl 会原样放行，所以兼容。
     */
    private String avatarUrl(String key) {
        if (key == null || key.isBlank()) {
            return key;
        }
        return ossService.publicUrl(key);
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
        return R.ok(followService.followingList(currentUserId(), com.dianping.common.PageParam.page(page), com.dianping.common.PageParam.size(pageSize)));
    }

    /** GET /user/fans?page= —— 关注我的粉丝列表（含互关标记） */
    @GetMapping("/fans")
    public R<Map<String, Object>> fans(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(followService.fansList(currentUserId(), com.dianping.common.PageParam.page(page), com.dianping.common.PageParam.size(pageSize)));
    }

    /** GET /user/notify —— 消息列表 */
    @GetMapping("/notify")
    public R<Map<String, Object>> notify(@RequestParam(required = false) String category,
                                         @RequestParam(required = false) String subType,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(notifyService.list(currentUserId(), category, subType, com.dianping.common.PageParam.page(page), com.dianping.common.PageParam.size(pageSize)));
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

    /** POST /user/notify/read-all?category=&subType= —— 全部已读（category 可分 interact/announce，subType 再细分互动子类） */
    @PostMapping("/notify/read-all")
    public R<Void> notifyReadAll(@RequestParam(required = false) String category,
                                 @RequestParam(required = false) String subType) {
        // subType 之前前端传了但后端收不到也不处理，导致"在当前筛选下点全部已读"
        // 会把其它子类的未读一起清掉
        notifyService.readAll(currentUserId(), category, subType);
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
        // 局部更新：只改请求里出现的字段。
        // 原来"非空就覆盖"，导致只想换头像时也得把简介一起传全量，
        // 前端某处漏传就会把用户简介冲掉 —— 静默丢数据比报错更糟。
        if (req.nickname() != null && !req.nickname().isBlank()) {
            u.setNickname(req.nickname().trim());
        }
        // 头像存 key，不存完整 URL：URL 里带当前 Host/OSS 域名，
        // 存下来换域名就失效（见 avatarUrl 注释）
        if (req.avatar() != null && !req.avatar().isBlank()) {
            u.setAvatar(req.avatar().trim());
        }
        // 传空串 = 明确清空（undefined/null = 未修改，两者语义必须区分开）
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
        return R.ok(contentService.likedContents(me, com.dianping.common.PageParam.page(page), com.dianping.common.PageParam.size(pageSize)));
    }

    /** GET /user/favorites?page=&pageSize=&folderId= —— 我的收藏（可按收藏夹筛选） */
    @GetMapping("/favorites")
    public R<Map<String, Object>> favorites(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int pageSize,
                                            @RequestParam(defaultValue = "0") long folderId) {
        Long me = currentUserId();
        return R.ok(favoriteService.favoriteContents(me, folderId, com.dianping.common.PageParam.page(page), com.dianping.common.PageParam.size(pageSize)));
    }

    /** GET /favorite/folders —— 我的收藏夹列表（含各夹收藏数 + 未分类） */
    @GetMapping("/favorite/folders")
    public R<List<Map<String, Object>>> listFolders() {
        return R.ok(favoriteService.listFolders(currentUserId()));
    }

    /** POST /favorite/folder {name} —— 新建收藏夹 */
    @PostMapping("/favorite/folder")
    public R<Map<String, Object>> createFolder(@org.springframework.web.bind.annotation.RequestBody java.util.Map<String, String> body) {
        Long me = currentUserId();
        long id = favoriteService.createFolder(me, body == null ? null : body.get("name"));
        return R.ok(java.util.Collections.singletonMap("folderId", String.valueOf(id)));
    }

    /** PATCH /favorite/folder/{folderId} {name} —— 重命名收藏夹 */
    @org.springframework.web.bind.annotation.PatchMapping("/favorite/folder/{folderId}")
    public R<Void> renameFolder(@PathVariable Long folderId,
                                @org.springframework.web.bind.annotation.RequestBody java.util.Map<String, String> body) {
        favoriteService.renameFolder(currentUserId(), folderId, body == null ? null : body.get("name"));
        return R.ok();
    }

    /** DELETE /favorite/folder/{folderId} —— 删除收藏夹（夹内收藏回落"未分类"，不删笔记） */
    @org.springframework.web.bind.annotation.DeleteMapping("/favorite/folder/{folderId}")
    public R<Void> deleteFolder(@PathVariable Long folderId) {
        favoriteService.deleteFolder(currentUserId(), folderId);
        return R.ok();
    }

    /** GET /user/browse?page=&pageSize= —— 我的浏览记录（覆盖式去重，按最近浏览倒序） */
    @GetMapping("/browse")
    public R<Map<String, Object>> browse(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "20") int pageSize) {
        Long me = currentUserId();
        return R.ok(contentService.browseList(me, com.dianping.common.PageParam.page(page), com.dianping.common.PageParam.size(pageSize)));
    }

    /** POST /user/{id}/block —— 拉黑 */
    @PostMapping("/{id}/block")
    public R<Void> block(@PathVariable Long id) {
        blockService.block(currentUserId(), id);
        return R.ok();
    }

    /** DELETE /user/{id}/block —— 取消拉黑 */
    @org.springframework.web.bind.annotation.DeleteMapping("/{id}/block")
    public R<Void> unblock(@PathVariable Long id) {
        blockService.unblock(currentUserId(), id);
        return R.ok();
    }

    /** GET /user/blocks —— 我的黑名单 */
    @GetMapping("/blocks")
    public R<List<Map<String, Object>>> blocks() {
        return R.ok(blockService.listBlocks(currentUserId()));
    }

    /** GET /user/followed-topics —— 我关注的话题列表 */
    @GetMapping("/followed-topics")
    public R<List<String>> followedTopics() {
        User u = userMapper.selectById(currentUserId());
        return R.ok(parseTopics(u == null ? null : u.getFollowedTopics()));
    }

    /** POST /user/follow-topic {topic} —— 关注话题 */
    @PostMapping("/follow-topic")
    public R<Void> followTopic(@org.springframework.web.bind.annotation.RequestBody java.util.Map<String, String> body) {
        String t = body == null ? null : body.get("topic");
        if (t == null || t.trim().isEmpty()) return R.ok();
        t = t.trim().replaceAll("^#+", "").replaceAll("#$", "");
        User u = userMapper.selectById(currentUserId());
        List<String> topics = parseTopics(u.getFollowedTopics());
        if (!topics.contains(t) && topics.size() < 50) {
            topics.add(t);
            u.setFollowedTopics(toJson(topics));
            userMapper.updateById(u);
        }
        return R.ok();
    }

    /** DELETE /user/follow-topic/{topic} —— 取消关注（topic 经 URL 编码） */
    @org.springframework.web.bind.annotation.DeleteMapping("/follow-topic/{topic}")
    public R<Void> unfollowTopic(@PathVariable String topic) {
        String t = java.net.URLDecoder.decode(topic, java.nio.charset.StandardCharsets.UTF_8);
        User u = userMapper.selectById(currentUserId());
        List<String> topics = parseTopics(u.getFollowedTopics());
        if (topics.remove(t)) {
            u.setFollowedTopics(toJson(topics));
            userMapper.updateById(u);
        }
        return R.ok();
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
        profile.put("avatar", avatarUrl(u.getAvatar()));
        profile.put("role", u.getRole());
        profile.put("status", u.getStatus());
        profile.put("phoneMasked", maskPhone(u.getPhone()));
        profile.put("bio", u.getBio() == null ? "" : u.getBio());
        profile.put("followers", followService.followersCount(u.getId()));
        Long meId0 = currentUserId();
        profile.put("following", followService.isFollowing(meId0, u.getId()));
        Long me = currentUserId();
        Long meId = me != null ? me : 0L;
        return R.ok(Map.of(
                "profile", profile,
                "contents", contentService.userContents(meId, userId, com.dianping.common.PageParam.page(page), com.dianping.common.PageParam.size(pageSize))));
    }

    private Long currentUserId() {
        Long id = com.dianping.common.UserContext.userId();
        if (id == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return id;
    }

    /** 解析关注话题 JSON 数组（容错：空/非法返回空列表） */
    private List<String> parseTopics(String raw) {
        if (raw == null || raw.isBlank()) return new ArrayList<>();
        try {
            String[] arr = new com.fasterxml.jackson.databind.ObjectMapper().readValue(raw, String[].class);
            return new ArrayList<>(java.util.List.of(arr));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /** 序列化为 JSON 数组字符串 */
    private String toJson(List<String> topics) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(topics);
        } catch (Exception e) {
            return "[]";
        }
    }
}
