package com.ecoswap.chat;

import com.ecoswap.auth.security.AuthenticatedUser;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Student-to-student chat for accepted exchanges (role USER; see SecurityConfig). */
@RestController
@RequestMapping("/api/chats")
public class ChatController {

    private final ChatService chats;

    public ChatController(ChatService chats) {
        this.chats = chats;
    }

    @GetMapping
    public List<ChatViews.Conversation> conversations(@AuthenticationPrincipal AuthenticatedUser me) {
        return chats.conversations(me.getId());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unread(@AuthenticationPrincipal AuthenticatedUser me) {
        return Map.of("unread", chats.unreadCount(me.getId()));
    }

    @GetMapping("/{requestId:\\d+}")
    public ChatViews.Thread thread(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long requestId,
                                   @RequestParam(required = false) Long after) {
        return chats.thread(me.getId(), requestId, after);
    }

    @GetMapping("/{requestId:\\d+}/older")
    public List<ChatViews.Message> older(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long requestId,
                                         @RequestParam Long before) {
        return chats.older(me.getId(), requestId, before);
    }

    @PostMapping("/{requestId:\\d+}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatViews.Message send(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long requestId,
                                  @RequestBody ChatViews.SendBody body) {
        return chats.send(me.getId(), requestId, body == null ? null : body.body());
    }
}
