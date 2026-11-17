package com.ecoswap.item.entity;

/** Moderation state of a listing. Kept separate from {@link ItemAvailabilityStatus}. */
public enum ListingApprovalStatus { PENDING_APPROVAL, APPROVED, REJECTED, NEEDS_CHANGES, SUSPENDED }
