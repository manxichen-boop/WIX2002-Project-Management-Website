package com.ecoswap.notification;

import com.ecoswap.chat.ChatMessageRepository;
import com.ecoswap.common.exception.NotFoundException;
import com.ecoswap.common.response.PageResponse;
import com.ecoswap.exchange.entity.ExchangeRequestStatus;
import com.ecoswap.exchange.repository.ExchangeRequestRepository;
import com.ecoswap.user.entity.AccountStatus;
import com.ecoswap.user.entity.User;
import com.ecoswap.user.entity.UserRole;
import com.ecoswap.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates in-app notifications (inside the caller's transaction) and serves them to their owner. */
@Service
public class NotificationService {

    private final NotificationRepository notifications;
    private final UserRepository users;
    private final ExchangeRequestRepository requests;
    private final ChatMessageRepository chatMessages;

    public NotificationService(NotificationRepository notifications, UserRepository users, ExchangeRequestRepository requests,
                               ChatMessageRepository chatMessages) {
        this.chatMessages = chatMessages;
        this.notifications = notifications;
        this.users = users;
        this.requests = requests;
    }

    @Transactional
    public void notify(User user, NotificationType type, String title, String message, String linkUrl) {
        notifications.save(new Notification(user, type, truncate(title, 150), truncate(message, 500), linkUrl));
    }

    /** Sends the same notification to every active administrator. */
    @Transactional
    public void notifyAdmins(NotificationType type, String title, String message, String linkUrl) {
        users.findByRoleAndAccountStatus(UserRole.ADMIN, AccountStatus.ACTIVE)
                .forEach(admin -> notify(admin, type, title, message, linkUrl));
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationView> list(Long userId, boolean unreadOnly, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        var result = unreadOnly ? notifications.findByUserIdAndReadFalse(userId, pageable) : notifications.findByUserId(userId, pageable);
        return PageResponse.of(result, NotificationView::of);
    }

    @Transactional
    public NotificationView markRead(Long userId, Long notificationId) {
        Notification n = notifications.findById(notificationId)
                .filter(x -> x.getUser().getId().equals(userId))     // users can only touch their own
                .orElseThrow(() -> new NotFoundException("Notification"));
        n.markRead();
        return NotificationView.of(n);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notifications.markAllRead(userId);
    }

    @Transactional(readOnly = true)
    public NotificationCounts counts(Long userId) {
        return new NotificationCounts(notifications.countByUserIdAndReadFalse(userId),
                requests.countByItemOwnerIdAndStatus(userId, ExchangeRequestStatus.PENDING),
                chatMessages.countUnreadFor(userId));
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }
}
