package com.ecoswap.exchange.dto;

import com.ecoswap.exchange.entity.ExchangeRequestStatus;
import com.ecoswap.item.entity.ListingType;
import java.time.Instant;

/**
 * An exchange request as seen by one participant (or an admin). The allowed actions are computed
 * on the server; contact e-mails are only revealed to the two participants once accepted.
 */
public record ExchangeRequestView(
        Long id, ExchangeRequestStatus status, String statusReason, String message,
        Instant createdAt, Instant updatedAt, Instant completedAt,
        ItemSummary item, ItemSummary offeredItem, Person owner, Person requester,
        String viewerRole, boolean ownerConfirmed, boolean requesterConfirmed, Actions actions) {

    public record ItemSummary(Long id, String name, String coverUrl, ListingType listingType, String categoryName, String pickupLocation) {}

    public record Person(Long id, String name, String university, String initials, String contactEmail) {}

    public record Actions(boolean accept, boolean reject, boolean cancel, boolean withdraw, boolean confirm) {}
}
