package com.ecoswap.report;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Report a listing (itemId) or a student (reportedUserId). At least one target is required. */
public record CreateReportBody(Long itemId, Long reportedUserId,
                               @NotNull(message = "Choose a reason.") ReportReason reason,
                               @Size(max = 1000, message = "Use 1000 characters or fewer.") String description) {}
