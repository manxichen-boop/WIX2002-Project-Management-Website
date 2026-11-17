package com.ecoswap.item.dto;

import com.ecoswap.item.entity.ItemAvailabilityStatus;
import com.ecoswap.item.entity.ListingApprovalStatus;
import com.ecoswap.item.entity.ListingType;
import java.time.Instant;

/** Compact listing used by cards and tables (Explore, homepage, saved items, dashboards). */
public record ItemCardView(
        Long id,
        String name,
        String categoryName,
        String categorySlug,
        String condition,
        String conditionLabel,
        ListingType listingType,
        String pickupLocation,
        java.math.BigDecimal pickupLatitude,
        java.math.BigDecimal pickupLongitude,
        String categoryIconKey,
        String coverUrl,
        String ownerFirstName,
        ListingApprovalStatus approvalStatus,
        ItemAvailabilityStatus availabilityStatus,
        Instant createdAt,
        boolean saved) {

    public ItemCardView withSaved(boolean value) {
        return new ItemCardView(id, name, categoryName, categorySlug, condition, conditionLabel, listingType, pickupLocation,
                pickupLatitude, pickupLongitude, categoryIconKey, coverUrl, ownerFirstName, approvalStatus, availabilityStatus, createdAt, value);
    }
}
