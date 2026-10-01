package com.dianping.module.wechat;

import com.dianping.common.R;
import com.dianping.common.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wechat")
@RequiredArgsConstructor
public class WechatController {

    private final WechatPushService wechatPushService;

    /** POST /wechat/subscribe/report  body: {"keys":["audit","interact"]} —— 前端授权成功后上报额度 */
    @PostMapping("/subscribe/report")
    public R<Void> reportSubscribe(@RequestBody Map<String, Object> body) {
        Object keysObj = body == null ? null : body.get("keys");
        List<String> keys = keysObj instanceof List
                ? ((List<?>) keysObj).stream().map(String::valueOf).toList()
                : List.of();
        wechatPushService.reportGrants(UserContext.userId(), keys);
        return R.ok();
    }
}
