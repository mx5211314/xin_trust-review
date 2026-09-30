package com.dianping.controller;

import com.dianping.common.R;
import com.dianping.common.RequireRole;
import com.dianping.dto.ContentVO;
import com.dianping.dto.RejectReq;
import com.dianping.service.AdminService;
import com.dianping.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台接口，全部仅 ADMIN 可访问（@RequireRole 注解 + AuthInterceptor 校验）。
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ContentService contentService;

    /** GET /admin/content/list?status=&page=&pageSize= —— 返回 VO（含完整图片 URL） */
    @GetMapping("/content/list")
    @RequireRole({"ADMIN"})
    public R<Object> contentList(@RequestParam(required = false) String status,
                                 @RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(contentService.adminVoPage(status, page, pageSize));
    }

    /** GET /admin/content/{id} —— 后台看单条详情（任意状态） */
    @GetMapping("/content/{id}")
    @RequireRole({"ADMIN"})
    public R<ContentVO> detail(@PathVariable Long id) {
        return R.ok(contentService.adminDetail(id));
    }

    /** POST /admin/content/{id}/approve */
    @PostMapping("/content/{id}/approve")
    @RequireRole({"ADMIN"})
    public R<Void> approve(@PathVariable Long id) {
        adminService.approve(id);
        return R.ok();
    }

    /** POST /admin/content/{id}/reject  body: {"reason":"..."} */
    @PostMapping("/content/{id}/reject")
    @RequireRole({"ADMIN"})
    public R<Void> reject(@PathVariable Long id, @Valid @RequestBody RejectReq req) {
        adminService.reject(id, req.reason());
        return R.ok();
    }

    /** POST /admin/content/{id}/takedown */
    @PostMapping("/content/{id}/takedown")
    @RequireRole({"ADMIN"})
    public R<Void> takedown(@PathVariable Long id) {
        adminService.takedown(id);
        return R.ok();
    }

    /** GET /admin/user/list?keyword= */
    @GetMapping("/user/list")
    @RequireRole({"ADMIN"})
    public R<Object> userList(@RequestParam(required = false) String keyword,
                              @RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(adminService.userList(keyword, page, pageSize));
    }

    /** POST /admin/user/{id}/ban */
    @PostMapping("/user/{id}/ban")
    @RequireRole({"ADMIN"})
    public R<Void> ban(@PathVariable Long id) {
        adminService.ban(id);
        return R.ok();
    }

    /** POST /admin/user/{id}/unban */
    @PostMapping("/user/{id}/unban")
    @RequireRole({"ADMIN"})
    public R<Void> unban(@PathVariable Long id) {
        adminService.unban(id);
        return R.ok();
    }

    /** POST /admin/reviewer/grant?userId= */
    @PostMapping("/reviewer/grant")
    @RequireRole({"ADMIN"})
    public R<Void> grant(@RequestParam Long userId) {
        adminService.grantReviewer(userId);
        return R.ok();
    }

    /** POST /admin/reviewer/revoke?userId= */
    @PostMapping("/reviewer/revoke")
    @RequireRole({"ADMIN"})
    public R<Void> revoke(@RequestParam Long userId) {
        adminService.revokeReviewer(userId);
        return R.ok();
    }
}
