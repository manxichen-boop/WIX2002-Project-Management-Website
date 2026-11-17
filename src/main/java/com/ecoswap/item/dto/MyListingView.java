package com.ecoswap.item.dto;

/** A listing in "My Listings": the card plus moderation feedback and request activity. */
public record MyListingView(ItemCardView item, String rejectionReason, long pendingRequests, boolean editable, boolean archivable) {}
