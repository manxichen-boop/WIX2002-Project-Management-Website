package com.ecoswap.notification;

import com.ecoswap.auth.security.AuthenticatedUser;
import com.ecoswap.common.response.MessageResponse;
import com.ecoswap.common.response.PageResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<NotificationView> list(@AuthenticationPrincipal AuthenticatedUser me,
                                               @RequestParam(defaultValue = "false") boolean unreadOnly,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return service.list(me.getId(), unreadOnly, page, size);
    }

    @GetMapping("/counts")
    public NotificationCounts counts(@AuthenticationPrincipal AuthenticatedUser me) {
        return service.counts(me.getId());
    }

    @PatchMapping("/{id}/read")
    public NotificationView markRead(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id) {
        return service.markRead(me.getId(), id);
    }

    @PatchMapping("/read-all")
    public MessageResponse markAllRead(@AuthenticationPrincipal AuthenticatedUser me) {
        int updated = service.markAllRead(me.getId());
        return new MessageResponse(updated == 1 ? "1 notification marked as read." : updated + " notifications marked as read.");
    }
}
