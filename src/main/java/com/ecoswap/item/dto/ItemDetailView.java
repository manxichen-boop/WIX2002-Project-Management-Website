package com.ecoswap.item.dto;

import com.ecoswap.item.entity.ItemAvailabilityStatus;
import com.ecoswap.item.entity.ListingApprovalStatus;
import com.ecoswap.item.entity.ListingType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Full listing for the details and edit pages. Owner information is limited to what other
 * students need (first name, university, initials) — never the e-mail address.
 */
public record ItemDetailView(
        Long id, String name, String description,
        Long categoryId, String categoryName, String categorySlug, String categoryIconKey,
        String condition, String conditionLabel, ListingType listingType,
        String pickupLocation, BigDecimal pickupLatitude, BigDecimal pickupLongitude, BigDecimal approximateWeight,
        ListingApprovalStatus approvalStatus, ItemAvailabilityStatus availabilityStatus, String rejectionReason,
        Instant createdAt, Instant updatedAt,
        List<ImageView> images,
        Owner owner,
        Viewer viewer) {

    public record Owner(Long id, String firstName, String university, String initials, String avatarUrl, Instant memberSince) {}

    /**
     * What the current visitor may do. requestBlockedReason explains why the request button is
     * disabled (own item, not available, already requested, not logged in…).
     */
    public record Viewer(boolean loggedIn, boolean owner, boolean admin, boolean saved, boolean canRequest,
                         String requestBlockedReason, Long activeRequestId, String activeRequestStatus) {}
}
