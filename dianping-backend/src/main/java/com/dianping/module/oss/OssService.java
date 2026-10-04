package com.dianping.module.oss;

import com.dianping.common.BizException;
import com.dianping.common.ResultCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 文件存储服务，双模式（dianping.oss.mode 配置切换）：
 *
 * local  ：开发期本地磁盘存储。客户端把文件传给后端（/oss/upload-local），
 *          落盘到 {local-dir}/upload/...，通过 /upload/** 静态映射对外访问。
 *          优点：零成本零依赖；缺点：文件在服务器本地，多机部署/换机要迁移。
 *
 * aliyun ：正式阿里云 OSS PostObject 表单直传（零第三方依赖，纯 JDK 签名）。
 *          客户端拿 upload-params 后直传 OSS，不经后端中转。
 *          生产环境增强建议：改用 RAM 角色 + STS 临时凭证签名，避免长期 AK 落配置文件。
 *
 * 两种模式对客户端完全透明：upload-params 返回的 host/key 不同而已，前端代码不变。
 */
@Service
public class OssService {

    private static final Set<String> IMAGE_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> VIDEO_EXT = Set.of("mp4", "mov", "m4v");

    /** local | aliyun */
    @Value("${dianping.oss.mode:aliyun}")
    private String mode;

    /** true=按请求 Host 动态拼资源地址（本地/真机调试用）；false=用固定 local-public-url（生产用，防 Host 头注入） */
    @Value("${dianping.oss.dynamic-host:true}")
    private boolean dynamicHost;

    @Value("${dianping.oss.local-dir:D:/dianping-upload}")
    private String localDir;
    @Value("${dianping.oss.local-public-url:http://localhost:18080}")
    private String localPublicUrl;

    @Value("${dianping.oss.access-key-id}")
    private String accessKeyId;
    @Value("${dianping.oss.access-key-secret}")
    private String accessKeySecret;
    @Value("${dianping.oss.endpoint}")
    private String endpoint;
    @Value("${dianping.oss.bucket}")
    private String bucket;
    @Value("${dianping.oss.public-host}")
    private String publicHost;
    @Value("${dianping.oss.image-max-size}")
    private long imageMaxSize;
    @Value("${dianping.oss.video-max-size}")
    private long videoMaxSize;
    @Value("${dianping.oss.policy-expire-minutes}")
    private int policyExpireMinutes;

    /**
     * 生成直传参数。
     *
     * @param type image | video
     * @param ext  扩展名，如 jpg / png / mp4
     */
    public Map<String, Object> uploadParams(String type, String ext, Long userId) {
        if ("local".equals(mode)) {
            return localParams(type, ext, userId);
        }
        return aliyunParams(type, ext, userId);
    }

    /** local 模式：host 指向后端中转端点，前端 uni.uploadFile 照常上传 */
    private Map<String, Object> localParams(String type, String ext, Long userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("mode", "local");
        data.put("host", currentBaseUrl() + "/oss/upload-local");
        data.put("key", buildKey(type, userId, ext));
        data.put("policy", "");
        data.put("ossAccessKeyId", "");
        data.put("signature", "");
        data.put("successActionStatus", "200");
        data.put("expire", Instant.now().plusSeconds(3600).getEpochSecond());
        return data;
    }

    private Map<String, Object> aliyunParams(String type, String ext, Long userId) {
        long maxSize;
        String dir;
        if ("video".equals(type)) {
            maxSize = videoMaxSize;
            dir = "content/video/" + userId + "/";
        } else {
            maxSize = imageMaxSize;
            dir = "content/image/" + userId + "/";
        }
        String key = dir + Instant.now().toEpochMilli() + "-" + java.util.UUID.randomUUID().toString().substring(0, 8)
                + (ext == null || ext.isBlank() ? "" : "." + ext.toLowerCase());

        String expiration = Instant.now().plusSeconds(policyExpireMinutes * 60L).toString();
        String policyJson = "{\"expiration\":\"" + expiration + "\",\"conditions\":[" +
                "{\"bucket\":\"" + bucket + "\"}," +
                "[\"content-length-range\",1," + maxSize + "]," +
                "[\"starts-with\",\"$key\",\"" + dir + "\"]" +
                "]}";
        String policy = Base64.getEncoder().encodeToString(policyJson.getBytes(StandardCharsets.UTF_8));
        String signature = hmacSha1(policy, accessKeySecret);

        Map<String, Object> data = new HashMap<>();
        data.put("mode", "aliyun");
        data.put("host", "https://" + bucket + "." + endpoint.replace("https://", ""));
        data.put("key", key);
        data.put("policy", policy);
        data.put("ossAccessKeyId", accessKeyId);
        data.put("signature", signature);
        data.put("successActionStatus", "200");
        data.put("expire", Instant.now().plusSeconds(policyExpireMinutes * 60L).getEpochSecond());
        return data;
    }

    /** local 模式中转上传：校验类型/大小/路径安全后落盘到 {local-dir}/upload/... */
    public void saveLocal(MultipartFile file, String key, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR, "文件为空");
        }
        if (key == null || !key.startsWith("upload/") || key.contains("..")) {
            throw new BizException(ResultCode.PARAM_ERROR, "非法的文件 key");
        }
        // 归属校验：只能写自己目录下的文件，防止用伪造 key 覆盖他人文件
        if (userId != null && !key.contains("/" + userId + "/")) {
            throw new BizException(ResultCode.FORBIDDEN, "非法的文件 key");
        }
        boolean isVideo = key.contains("/video/");
        String ext = extOf(key);
        Set<String> allowed = isVideo ? VIDEO_EXT : IMAGE_EXT;
        if (!allowed.contains(ext)) {
            throw new BizException(ResultCode.PARAM_ERROR, "不支持的文件类型：" + ext);
        }
        long max = isVideo ? videoMaxSize : imageMaxSize;
        if (file.getSize() > max) {
            throw new BizException(ResultCode.PARAM_ERROR, isVideo ? "视频超过 100MB" : "图片超过 10MB");
        }
        try {
            Path dest = Paths.get(localDir, key).normalize();
            if (!dest.startsWith(Paths.get(localDir).normalize())) {
                throw new BizException(ResultCode.PARAM_ERROR, "非法的文件 key");
            }
            Files.createDirectories(dest.getParent());
            file.transferTo(dest.toFile());
        } catch (IOException e) {
            throw new BizException(ResultCode.SYSTEM_ERROR, "保存文件失败");
        }
    }

    /** 把 objectKey 拼成完整可访问 URL（信息流/详情返回给客户端用） */
    public String publicUrl(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        // 演示数据允许直接存完整外链（如 picsum 示例图），原样返回
        if (key.startsWith("http://") || key.startsWith("https://")) {
            return key;
        }
        if ("local".equals(mode)) {
            // 动态跟随请求 Host：浏览器访问 localhost、真机访问局域网 IP，图片都能加载
            return currentBaseUrl() + "/" + key;
        }
        return publicHost + "/" + key;
    }

    /** 统一 key 生成：upload/{image|video}/{userId}/{时间戳}-{随机8位}.{ext} */
    private String buildKey(String type, Long userId, String ext) {
        String dir = "video".equals(type) ? "upload/video/" : "upload/image/";
        return dir + userId + "/" + Instant.now().toEpochMilli() + "-"
                + java.util.UUID.randomUUID().toString().substring(0, 8)
                + (ext == null || ext.isBlank() ? "" : "." + ext.toLowerCase());
    }

    private String extOf(String key) {
        int i = key.lastIndexOf('.');
        return i < 0 ? "" : key.substring(i + 1).toLowerCase();
    }

    /**
     * 动态取当前请求的 Host 作为 base：真机调试时手机访问的是局域网 IP，
     * 返回的上传地址自动跟随，不用手改配置。取不到时回退 local-public-url。
     */
    private String currentBaseUrl() {
        if (!dynamicHost) {
            return localPublicUrl;
        }
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                jakarta.servlet.http.HttpServletRequest req = attrs.getRequest();
                String host = req.getHeader("Host");
                if (host != null && !host.isBlank()) {
                    // 协议必须跟随实际请求：穿透/反代后页面是 https，
                    // 若这里写死 http，浏览器会按「混合内容」拦掉上传和图片
                    // （表现就是上传报「请检查网络」、图片不显示）
                    String scheme = req.getHeader("X-Forwarded-Proto");
                    if (scheme == null || scheme.isBlank()) {
                        scheme = req.getScheme();
                    }
                    return scheme + "://" + host;
                }
            }
        } catch (Exception ignored) {
        }
        return localPublicUrl;
    }

    private String hmacSha1(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new BizException(ResultCode.SYSTEM_ERROR, "签名失败");
        }
    }
}
