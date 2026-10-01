package com.dianping.module.wechat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dianping.module.user.User;
import com.dianping.module.user.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 微信小程序订阅消息推送。
 * 外部依赖强：需要小程序 AppID/Secret + 订阅消息模板 ID + 用户 openid。
 * 未配置（默认）时全部方法为 no-op，只打 debug 日志，不影响任何主流程；
 * 配置后自动启用：NotifyService 触发 → 有额度且有 openid → 调微信接口下发。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatPushService {

    private final UserMapper userMapper;
    private final SubscribeGrantMapper grantMapper;
    private final StringRedisTemplate redis;

    @Value("${dianping.wechat.appid:}")
    private String appid;
    @Value("${dianping.wechat.secret:}")
    private String secret;
    @Value("${dianping.wechat.template-audit:}")
    private String templateAudit;
    @Value("${dianping.wechat.template-interact:}")
    private String templateInteract;

    private static final String TOKEN_KEY = "wx:access_token";

    /** 前端 requestSubscribeMessage 授权成功后上报：额度 +1 */
    public void reportGrants(Long me, List<String> keys) {
        if (keys == null) return;
        for (String k : keys) {
            if (k == null || k.isBlank()) continue;
            String key = k.trim();
            SubscribeGrant g = grantMapper.selectOne(new LambdaQueryWrapper<SubscribeGrant>()
                    .eq(SubscribeGrant::getUserId, me)
                    .eq(SubscribeGrant::getTemplateKey, key));
            if (g == null) {
                g = new SubscribeGrant();
                g.setUserId(me);
                g.setTemplateKey(key);
                g.setGrantCount(1);
                g.setUpdateTime(LocalDateTime.now());
                grantMapper.insert(g);
            } else {
                g.setGrantCount(Math.min(g.getGrantCount() + 1, 10)); // 本地记账上限，防刷
                g.setUpdateTime(LocalDateTime.now());
                grantMapper.updateById(g);
            }
        }
    }

    /** 消耗一条额度；无额度返回 false */
    private boolean consumeQuota(Long userId, String key) {
        SubscribeGrant g = grantMapper.selectOne(new LambdaQueryWrapper<SubscribeGrant>()
                .eq(SubscribeGrant::getUserId, userId)
                .eq(SubscribeGrant::getTemplateKey, key));
        if (g == null || g.getGrantCount() == null || g.getGrantCount() <= 0) {
            return false;
        }
        g.setGrantCount(g.getGrantCount() - 1);
        g.setUpdateTime(LocalDateTime.now());
        grantMapper.updateById(g);
        return true;
    }

    /**
     * 异步下发订阅消息（未配置/无 openid/无额度均静默跳过）。
     * data 的字段名(thing1/time2 等)需与所选模板的字段一致，接入时按实际模板调整。
     */
    @Async
    public void pushAsync(Long userId, String templateKey, String page, Map<String, String> data) {
        try {
            if (!configured()) {
                log.debug("[wechat] 订阅消息未配置，跳过 key={} user={}", templateKey, userId);
                return;
            }
            String tmplId = templateIdOf(templateKey);
            if (tmplId == null || tmplId.isBlank()) {
                log.debug("[wechat] 模板 {} 未配置模板ID，跳过", templateKey);
                return;
            }
            User u = userMapper.selectById(userId);
            if (u == null || u.getOpenid() == null || u.getOpenid().isBlank()) {
                log.debug("[wechat] 用户 {} 无 openid（需接入微信登录），跳过", userId);
                return;
            }
            if (!consumeQuota(userId, templateKey)) {
                log.debug("[wechat] 用户 {} 模板 {} 无授权额度，跳过", userId, templateKey);
                return;
            }
            String token = accessToken();
            sendSubscribe(token, u.getOpenid(), tmplId, page, data);
        } catch (Exception e) {
            log.warn("[wechat] 订阅消息发送失败（不影响主流程）: {}", e.getMessage());
        }
    }

    private boolean configured() {
        return appid != null && !appid.isBlank() && secret != null && !secret.isBlank();
    }

    private String templateIdOf(String key) {
        return "audit".equals(key) ? templateAudit : templateInteract;
    }

    /** access_token：Redis 缓存 3500s，过期重新拉 */
    private String accessToken() throws Exception {
        String cached = redis.opsForValue().get(TOKEN_KEY);
        if (cached != null && !cached.isBlank()) {
            return cached;
        }
        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="
                + appid + "&secret=" + secret;
        String body = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(url).normalize())
                        .GET().build(),
                java.net.http.HttpResponse.BodyHandlers.ofString()).body();
        Map<String, Object> m = new com.fasterxml.jackson.databind.ObjectMapper().readValue(body, Map.class);
        Object token = m.get("access_token");
        if (token == null) {
            throw new IllegalStateException("获取 access_token 失败: " + body);
        }
        redis.opsForValue().set(TOKEN_KEY, String.valueOf(token), Duration.ofSeconds(3500));
        return String.valueOf(token);
    }

    /** POST subscribe/send；43101=用户拒绝接收 → 清额度记录 */
    @SuppressWarnings("unchecked")
    private void sendSubscribe(String token, String openid, String tmplId, String page, Map<String, String> data) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("touser", openid);
        payload.put("template_id", tmplId);
        if (page != null && !page.isBlank()) {
            payload.put("page", page);
        }
        Map<String, Object> dataWrap = new HashMap<>();
        for (Map.Entry<String, String> e : data.entrySet()) {
            Map<String, String> v = new HashMap<>();
            v.put("value", e.getValue() == null ? "" : e.getValue());
            dataWrap.put(e.getKey(), v);
        }
        payload.put("data", dataWrap);
        String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder(
                        java.net.URI.create("https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + token))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json))
                .build();
        String body = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString()).body();
        Map<String, Object> resp = new com.fasterxml.jackson.databind.ObjectMapper().readValue(body, Map.class);
        int errcode = resp.get("errcode") == null ? -1 : ((Number) resp.get("errcode")).intValue();
        if (errcode == 0) {
            log.info("[wechat] 订阅消息已下发 openid={} tmpl={}", openid, tmplId);
        } else if (errcode == 43101) {
            log.debug("[wechat] 用户拒绝接收订阅消息，清记账额度 openid={}", openid);
            // 用户侧已拒收：把本地记账清零，避免下次空发
            SubscribeGrant g = grantMapper.selectOne(new LambdaQueryWrapper<SubscribeGrant>()
                    .eq(SubscribeGrant::getUserId, userIdOf(openid))
                    .eq(SubscribeGrant::getTemplateKey, templateKeyOf(tmplId)));
            if (g != null) {
                g.setGrantCount(0);
                g.setUpdateTime(LocalDateTime.now());
                grantMapper.updateById(g);
            }
        } else {
            log.warn("[wechat] 下发失败 errcode={} body={}", errcode, body);
        }
    }

    private Long userIdOf(String openid) {
        User u = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getOpenid, openid).last("LIMIT 1"));
        return u == null ? 0L : u.getId();
    }

    private String templateKeyOf(String tmplId) {
        return tmplId.equals(templateAudit) ? "audit" : "interact";
    }
}
