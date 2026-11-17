package com.ecoswap.impact;

import java.time.Instant;

/**
 * One public activity entry. Deliberately contains no names or contact details (privacy):
 * only what changed hands, its category and when.
 */
public record CommunityActivity(String kind, String itemName, String categoryName, String listingType, Instant at, Long itemId) {}
