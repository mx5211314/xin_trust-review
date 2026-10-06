package com.dianping.module.chat;

import com.dianping.common.R;
import com.dianping.common.UserContext;
import com.dianping.module.chat.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /** POST /chat/send  body: {"toUserId":1001,"text":"..."} —— 发私信 */
    @PostMapping("/send")
    public R<Map<String, Object>> send(@RequestBody Map<String, Object> body) {
        Object to = body.get("toUserId");
        Object text = body.get("text");
        Object img = body.get("imageKey");
        Long toUserId = to == null ? null : Long.parseLong(String.valueOf(to));
        return R.ok(chatService.send(UserContext.userId(), toUserId,
                text == null ? "" : String.valueOf(text),
                img == null ? "" : String.valueOf(img)));
    }

    /** GET /chat/messages?userId=xxx —— 与某人的聊天记录 */
    @GetMapping("/messages")
    public R<Map<String, Object>> messages(@RequestParam Long userId,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(chatService.messages(UserContext.userId(), userId, com.dianping.common.PageParam.page(page), com.dianping.common.PageParam.size(pageSize)));
    }

    /** GET /chat/conversations —— 会话列表 */
    @GetMapping("/conversations")
    public R<Map<String, Object>> conversations(@RequestParam(defaultValue = "30") int limit) {
        return R.ok(chatService.conversations(UserContext.userId(), limit));
    }

    /** GET /chat/unread —— 未读私信总数 */
    @GetMapping("/unread")
    public R<Map<String, Object>> unread() {
        return R.ok(java.util.Collections.singletonMap("unread", chatService.unreadTotal(UserContext.userId())));
    }
}
