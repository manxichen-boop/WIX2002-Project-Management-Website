package com.ecoswap.notification;

import com.ecoswap.common.util.Times;
import java.time.Instant;

public record NotificationView(Long id, String title, String message, NotificationType type, String linkUrl, boolean read, Instant createdAt) {

    public static NotificationView of(Notification n) {
        return new NotificationView(n.getId(), n.getTitle(), n.getMessage(), n.getType(), n.getLinkUrl(), n.isRead(), Times.utc(n.getCreatedAt()));
    }
}
