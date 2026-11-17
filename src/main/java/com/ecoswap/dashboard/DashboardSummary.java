package com.ecoswap.dashboard;

import com.ecoswap.auth.dto.MeResponse;
import com.ecoswap.exchange.dto.ExchangeRequestView;
import com.ecoswap.notification.NotificationView;
import java.util.List;

/** Everything the student dashboard shows, from live database counts. */
public record DashboardSummary(MeResponse profile, Counts counts, List<ExchangeRequestView> awaitingYourAnswer,
                               List<NotificationView> recentNotifications) {

    public record Counts(long activeListings, long listingsInReview, long pendingSent, long pendingReceived,
                         long acceptedExchanges, long completedExchanges, long savedItems, long unreadNotifications) {}
}
