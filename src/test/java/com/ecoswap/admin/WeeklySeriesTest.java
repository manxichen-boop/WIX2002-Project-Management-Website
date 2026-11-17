package com.ecoswap.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.ecoswap.admin.service.AdminDashboardServiceAccess;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Admin chart series: timestamps are bucketed per ISO week, including empty weeks. */
class WeeklySeriesTest {

    @Test
    void bucketsByIsoWeekAndKeepsEmptyWeeks() {
        LocalDate firstMonday = LocalDate.of(2026, 9, 21);   // a Monday
        var points = AdminDashboardServiceAccess.weekly(List.of(
                LocalDateTime.of(2026, 9, 21, 9, 0), LocalDateTime.of(2026, 9, 27, 23, 59),   // week 1 (Mon..Sun)
                LocalDateTime.of(2026, 11, 10, 12, 0)), firstMonday);                        // week 8
        assertThat(points).hasSize(8);
        assertThat(points.get(0).count()).isEqualTo(2);
        assertThat(points.get(1).count()).isZero();
        assertThat(points.get(7).weekStart()).isEqualTo("2026-11-09");
        assertThat(points.get(7).count()).isEqualTo(1);
    }
}
