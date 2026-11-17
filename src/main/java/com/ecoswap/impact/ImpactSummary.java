package com.ecoswap.impact;

import java.math.BigDecimal;
import java.util.List;

/**
 * Verified platform figures, all computed from database rows.
 * reusedWeightKg is the sum of owner-recorded weights of exchanged items; items without a recorded
 * weight are counted in itemsReused but contribute nothing to the weight (no estimation).
 */
public record ImpactSummary(
        long communityMembers,
        long completedExchanges,
        long itemsReused,
        BigDecimal reusedWeightKg,
        long itemsWithRecordedWeight,
        long availableListings,
        List<Breakdown> reusedByType,
        List<Breakdown> reusedByCategory) {

    public record Breakdown(String label, long count) {}

    /** Largest value in each breakdown, used to scale the bar charts (at least 1 to avoid dividing by zero). */
    public long maxReusedByType() {
        return reusedByType.stream().mapToLong(Breakdown::count).max().orElse(1);
    }

    public long maxReusedByCategory() {
        return reusedByCategory.stream().mapToLong(Breakdown::count).max().orElse(1);
    }
}
