package com.ecoswap.admin.service;

import com.ecoswap.admin.dto.AdminDashboardView;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Test-only bridge to the package-private weekly bucketing helper. */
public final class AdminDashboardServiceAccess {

    private AdminDashboardServiceAccess() {}

    public static List<AdminDashboardView.WeekPoint> weekly(List<LocalDateTime> times, LocalDate firstMonday) {
        return AdminDashboardService.weekly(times, firstMonday);
    }
}
