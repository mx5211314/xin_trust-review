package com.dianping.controller;

import com.dianping.common.R;
import com.dianping.service.OssService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/oss")
@RequiredArgsConstructor
public class OssController {

    private final OssService ossService;

    /**
     * GET /oss/upload-params?type=image|video&ext=jpg
     * 返回上传参数：
     * - aliyun 模式：PostObject 直传参数，客户端直传 OSS，不经后端中转
     * - local  模式：host 指向 /oss/upload-local，由后端落盘
     */
    @GetMapping("/upload-params")
    public R<Map<String, Object>> uploadParams(@RequestParam String type,
                                               @RequestParam(required = false) String ext) {
        Long userId = com.dianping.common.UserContext.userId();
        return R.ok(ossService.uploadParams(type, ext, userId));
    }

    /**
     * POST /oss/upload-local —— local 模式的中转上传端点。
     * multipart 字段：file（文件本体）、key（来自 upload-params，防客户端自定义路径）
     */
    @PostMapping("/upload-local")
    public R<Void> uploadLocal(@RequestParam("file") MultipartFile file,
                               @RequestParam("key") String key) {
        ossService.saveLocal(file, key, com.dianping.common.UserContext.userId());
        return R.ok();
    }
}
