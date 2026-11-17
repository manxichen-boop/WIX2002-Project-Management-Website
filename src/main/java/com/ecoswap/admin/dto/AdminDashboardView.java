package com.ecoswap.admin.dto;

import com.ecoswap.audit.AuditLogView;
import java.util.List;

/** Admin overview: every number is a database count; series are per ISO week (oldest first). */
public record AdminDashboardView(Kpis kpis, List<WeekPoint> registrations, List<WeekPoint> newListings,
                                 List<WeekPoint> completedExchanges, List<AuditLogView> recentActions) {

    public record Kpis(long totalUsers, long pendingRegistrations, long activeUsers, long suspendedUsers,
                       long totalListings, long pendingListings, long approvedListings, long rejectedListings,
                       long availableItems, long reservedItems, long completedExchanges, long openReports, long openTickets) {}

    /** weekStart is the Monday of the week (yyyy-MM-dd). */
    public record WeekPoint(String weekStart, long count) {}
}
