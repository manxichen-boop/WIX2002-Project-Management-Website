package com.ecoswap.admin.dto;

import com.ecoswap.approval.ApprovalHistoryView;
import com.ecoswap.item.dto.ImageView;
import com.ecoswap.item.entity.ItemAvailabilityStatus;
import com.ecoswap.item.entity.ListingApprovalStatus;
import com.ecoswap.item.entity.ListingType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Everything an administrator needs to review a listing, including its owner and decision history. */
public record AdminItemView(
        Long id, String name, String description, String categoryName, String conditionLabel, ListingType listingType,
        String pickupLocation, BigDecimal approximateWeight, ListingApprovalStatus approvalStatus,
        ItemAvailabilityStatus availabilityStatus, String rejectionReason, Instant createdAt, Instant updatedAt,
        Long ownerId, String ownerName, String ownerEmail, String ownerUniversity,
        List<ImageView> images, List<ApprovalHistoryView> history) {}
