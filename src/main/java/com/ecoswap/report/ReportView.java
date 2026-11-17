package com.ecoswap.report;

import com.ecoswap.common.util.Times;
import java.time.Instant;

public record ReportView(Long id, ReportReason reason, String reasonLabel, String description, ReportStatus status,
                         String resolutionNotes, Instant createdAt, Instant updatedAt,
                         Long reporterId, String reporterName,
                         Long reportedUserId, String reportedUserName, String reportedUserStatus,
                         Long itemId, String itemName, String itemApprovalStatus, String reviewedBy) {

    public static ReportView of(Report r) {
        var item = r.getItem();
        var user = r.getReportedUser();
        return new ReportView(r.getId(), r.getReason(), r.getReason().label(), r.getDescription(), r.getStatus(),
                r.getResolutionNotes(), Times.utc(r.getCreatedAt()), Times.utc(r.getUpdatedAt()),
                r.getReporter().getId(), r.getReporter().getFullName(),
                user == null ? null : user.getId(), user == null ? null : user.getFullName(),
                user == null ? null : user.getAccountStatus().name(),
                item == null ? null : item.getId(), item == null ? null : item.getName(),
                item == null ? null : item.getApprovalStatus().name(),
                r.getReviewedBy() == null ? null : r.getReviewedBy().getFullName());
    }
}
