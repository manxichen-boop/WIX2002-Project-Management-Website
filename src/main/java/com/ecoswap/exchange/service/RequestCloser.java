package com.ecoswap.exchange.service;

import com.ecoswap.exchange.entity.ExchangeRequest;
import com.ecoswap.exchange.entity.ExchangeRequestStatus;
import com.ecoswap.exchange.repository.ExchangeRequestRepository;
import com.ecoswap.item.entity.Item;
import com.ecoswap.notification.NotificationService;
import com.ecoswap.notification.NotificationType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shared clean-up when an item leaves circulation (archived, suspended, edited back into review):
 * pending requests involving it are CLOSED and accepted exchanges are CANCELLED, releasing the
 * other item, with notifications to everyone affected.
 */
@Component
public class RequestCloser {

    private final ExchangeRequestRepository requests;
    private final NotificationService notifications;

    public RequestCloser(ExchangeRequestRepository requests, NotificationService notifications) {
        this.requests = requests;
        this.notifications = notifications;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void closePendingFor(Item item, String reason) {
        for (ExchangeRequest r : requests.findPendingInvolvingItem(item.getId())) {
            r.finish(ExchangeRequestStatus.CLOSED, reason);
            notifications.notify(r.getRequester(), NotificationType.REQUEST_CLOSED, "Request closed",
                    "Your request for \"" + r.getItem().getName() + "\" was closed: " + reason, "/my/requests");
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void cancelAcceptedFor(Item item, String reason) {
        for (ExchangeRequest r : requests.findAcceptedInvolvingItem(item.getId())) {
            r.finish(ExchangeRequestStatus.CANCELLED, reason);
            if (r.getItem().getAvailabilityStatus() == com.ecoswap.item.entity.ItemAvailabilityStatus.RESERVED) {
                r.getItem().release();
            }
            if (r.getOfferedItem() != null
                    && r.getOfferedItem().getAvailabilityStatus() == com.ecoswap.item.entity.ItemAvailabilityStatus.RESERVED) {
                r.getOfferedItem().release();
            }
            String message = "The exchange for \"" + r.getItem().getName() + "\" was cancelled: " + reason;
            notifications.notify(r.getRequester(), NotificationType.REQUEST_CANCELLED, "Exchange cancelled", message, "/my/requests");
            notifications.notify(r.getItem().getOwner(), NotificationType.REQUEST_CANCELLED, "Exchange cancelled", message, "/my/received-requests");
        }
    }
}
