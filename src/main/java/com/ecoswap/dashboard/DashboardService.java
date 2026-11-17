package com.ecoswap.dashboard;

import com.ecoswap.auth.dto.MeResponse;
import com.ecoswap.common.exception.NotFoundException;
import com.ecoswap.exchange.entity.ExchangeRequestStatus;
import com.ecoswap.exchange.repository.ExchangeRequestRepository;
import com.ecoswap.exchange.service.ExchangeService;
import com.ecoswap.favorite.FavoriteRepository;
import com.ecoswap.item.entity.ItemAvailabilityStatus;
import com.ecoswap.item.entity.ListingApprovalStatus;
import com.ecoswap.item.repository.ItemRepository;
import com.ecoswap.notification.NotificationRepository;
import com.ecoswap.notification.NotificationService;
import com.ecoswap.user.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final UserRepository users;
    private final ItemRepository items;
    private final ExchangeRequestRepository requests;
    private final FavoriteRepository favorites;
    private final NotificationRepository notificationRepository;
    private final NotificationService notifications;
    private final ExchangeService exchanges;

    public DashboardService(UserRepository users, ItemRepository items, ExchangeRequestRepository requests, FavoriteRepository favorites,
                            NotificationRepository notificationRepository, NotificationService notifications, ExchangeService exchanges) {
        this.users = users;
        this.items = items;
        this.requests = requests;
        this.favorites = favorites;
        this.notificationRepository = notificationRepository;
        this.notifications = notifications;
        this.exchanges = exchanges;
    }

    @Transactional(readOnly = true)
    public DashboardSummary summary(Long userId) {
        var user = users.findById(userId).orElseThrow(() -> new NotFoundException("Account"));
        var counts = new DashboardSummary.Counts(
                items.countByOwnerIdAndApprovalStatusAndAvailabilityStatus(userId, ListingApprovalStatus.APPROVED, ItemAvailabilityStatus.AVAILABLE),
                items.countByOwnerIdAndApprovalStatusIn(userId, List.of(ListingApprovalStatus.PENDING_APPROVAL,
                        ListingApprovalStatus.NEEDS_CHANGES, ListingApprovalStatus.REJECTED)),
                requests.countByRequesterIdAndStatus(userId, ExchangeRequestStatus.PENDING),
                requests.countByItemOwnerIdAndStatus(userId, ExchangeRequestStatus.PENDING),
                requests.countParticipating(userId, ExchangeRequestStatus.ACCEPTED),
                requests.countParticipating(userId, ExchangeRequestStatus.COMPLETED),
                favorites.countByUserId(userId),
                notificationRepository.countByUserIdAndReadFalse(userId));
        return new DashboardSummary(MeResponse.of(user), counts,
                exchanges.received(userId, ExchangeRequestStatus.PENDING, 0, 3).content(),
                notifications.list(userId, false, 0, 5).content());
    }
}
