package com.dianping.module.interaction;

import com.dianping.common.R;
import com.dianping.common.RequireRole;
import com.dianping.common.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /** POST /report  body: {"targetType":"CONTENT|COMMENT|USER","targetId":123,"reason":"..."} */
    @PostMapping
    public R<Void> report(@RequestBody Map<String, Object> body) {
        Long me = UserContext.userId();
        String targetType = body.get("targetType") == null ? null : String.valueOf(body.get("targetType"));
        Object tid = body.get("targetId");
        Long targetId = tid == null ? null : Long.parseLong(String.valueOf(tid));
        String reason = body.get("reason") == null ? "" : String.valueOf(body.get("reason"));
        reportService.report(me, targetType, targetId, reason);
        return R.ok();
    }

    /** GET /report/admin/list?status=&page=&pageSize= —— 后台举报列表（仅管理员） */
    @GetMapping("/admin/list")
    @RequireRole({"ADMIN"})
    public R<Map<String, Object>> adminList(@RequestParam(required = false) String status,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(reportService.list(com.dianping.common.PageParam.page(page), com.dianping.common.PageParam.size(pageSize), status));
    }

    /** POST /report/admin/{id}/handle?status=HANDLED —— 后台标记处理（仅管理员） */
    @PostMapping("/admin/{id}/handle")
    @RequireRole({"ADMIN"})
    public R<Void> adminHandle(@PathVariable Long id,
                               @RequestParam(defaultValue = "HANDLED") String status) {
        reportService.handle(id, status);
        return R.ok();
    }
}
