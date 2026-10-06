package com.dianping.module.poi;

import com.dianping.common.R;
import com.dianping.common.RequireRole;
import com.dianping.common.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 门店接口。
 *
 * 注意：门店审核接口放在这里（而不是 AdminController），
 * 因为它的领域归属是门店；仅靠 @RequireRole 控制权限。
 */
@RestController
@RequestMapping("/poi")
@RequiredArgsConstructor
public class PoiController {

    private final PoiService poiService;
    /**
     * 门店页要列出该店的点评，而点评 VO 的组装逻辑在内容模块。
     * Controller 依赖 Service 不构成循环（ContentService 依赖的是 PoiService，不是本类）。
     */
    private final com.dianping.module.content.ContentService contentService;

    /** GET /poi/search?kw=&cityCode=&limit= —— 门店联想（含自己创建的待审门店） */
    @GetMapping("/search")
    public R<java.util.List<Map<String, Object>>> search(@RequestParam(required = false) String kw,
                                                         @RequestParam(required = false) String cityCode,
                                                         @RequestParam(required = false) Integer limit) {
        return R.ok(poiService.search(UserContext.userId(), kw, cityCode, limit));
    }

    /**
     * POST /poi —— 新建门店。
     * 管理员建的直接可用；点评人建的进 PENDING 待审（见 PoiService 类注释）。
     * 同名同城已存在时直接复用，返回已有 id。
     */
    @PostMapping
    @RequireRole({"REVIEWER"})
    public R<Map<String, Object>> create(@Valid @RequestBody PoiCreateReq req) {
        Long me = UserContext.userId();
        boolean isAdmin = "ADMIN".equals(UserContext.role());
        Long id = poiService.create(me, isAdmin, req);
        // 回读一次，把 status 一并给前端（好提示"待审核"）
        return R.ok(poiService.toVo(poiService.get(me, id), me));
    }

    /** GET /poi/{id} —— 门店详情 */
    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        Long me = UserContext.userId();
        return R.ok(poiService.toVo(poiService.get(me, id), me));
    }

    /**
     * GET /poi/{id}/contents?page=&pageSize= —— 门店页：该店的点评列表 + 门店信息。
     * 走 contentService 而不是 poiService：点评列表的组装（VO/拉黑过滤/可见性）
     * 都在内容模块，门店模块不该重复实现一遍。
     */
    @GetMapping("/{id}/contents")
    public R<Map<String, Object>> contents(@PathVariable Long id,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int pageSize) {
        Long me = UserContext.userId();
        return R.ok(contentService.poiContents(me, id,
                com.dianping.common.PageParam.page(page),
                com.dianping.common.PageParam.size(pageSize)));
    }

    // ---------- 管理端：门店审核 ----------

    /** GET /poi/admin/list?status=PENDING —— 待审门店列表 */
    @GetMapping("/admin/list")
    @RequireRole({"ADMIN"})
    public R<java.util.List<Map<String, Object>>> adminList(@RequestParam(required = false) String status) {
        return R.ok(poiService.adminList(status));
    }

    /** POST /poi/admin/{id}/approve —— 审核通过 */
    @PostMapping("/admin/{id}/approve")
    @RequireRole({"ADMIN"})
    public R<Void> approve(@PathVariable Long id) {
        poiService.audit(id, true, null);
        return R.ok();
    }

    /** POST /poi/admin/{id}/reject  body: {"reason":"..."} —— 审核驳回 */
    @PostMapping("/admin/{id}/reject")
    @RequireRole({"ADMIN"})
    public R<Void> reject(@PathVariable Long id,
                          @RequestBody(required = false) Map<String, Object> body) {
        String reason = body == null || body.get("reason") == null
                ? "" : String.valueOf(body.get("reason"));
        poiService.audit(id, false, reason);
        return R.ok();
    }

    /**
     * POST /poi/admin/migrate-from-names —— 存量迁移。
     * 把历史 `content.poi_name` 归并成门店记录并回填 `poi_id`。
     * 幂等，可重复调用；详见 PoiService.migrateFromNames 的注释（为什么不写成 SQL 脚本）。
     */
    @PostMapping("/admin/migrate-from-names")
    @RequireRole({"ADMIN"})
    public R<Map<String, Object>> migrateFromNames() {
        return R.ok(poiService.migrateFromNames(poiService.findMigratableContents()));
    }

    /** POST /poi/admin/recalc-counts —— 按 content.poi_id 重算各门店的笔记数（迁移后校正用） */
    @PostMapping("/admin/recalc-counts")
    @RequireRole({"ADMIN"})
    public R<Void> recalcCounts() {
        poiService.recalcContentCounts();
        return R.ok();
    }
}
