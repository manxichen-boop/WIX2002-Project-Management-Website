package com.ecoswap.admin.service;

import com.ecoswap.admin.dto.AdminDashboardView;
import com.ecoswap.audit.AuditService;
import com.ecoswap.exchange.entity.ExchangeRequestStatus;
import com.ecoswap.exchange.repository.ExchangeRequestRepository;
import com.ecoswap.item.entity.ItemAvailabilityStatus;
import com.ecoswap.item.entity.ListingApprovalStatus;
import com.ecoswap.item.repository.ItemRepository;
import com.ecoswap.report.ReportRepository;
import com.ecoswap.support.SupportTicketRepository;
import com.ecoswap.user.entity.AccountStatus;
import com.ecoswap.user.entity.UserRole;
import com.ecoswap.user.repository.UserRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardService {

    static final int WEEKS = 8;

    private final UserRepository users;
    private final ItemRepository items;
    private final ExchangeRequestRepository requests;
    private final ReportRepository reports;
    private final SupportTicketRepository tickets;
    private final AuditService audit;

    public AdminDashboardService(UserRepository users, ItemRepository items, ExchangeRequestRepository requests,
                                 ReportRepository reports, SupportTicketRepository tickets, AuditService audit) {
        this.users = users;
        this.items = items;
        this.requests = requests;
        this.reports = reports;
        this.tickets = tickets;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public AdminDashboardView dashboard() {
        var kpis = new AdminDashboardView.Kpis(
                users.countByRole(UserRole.USER),
                users.countByRoleAndAccountStatus(UserRole.USER, AccountStatus.PENDING_APPROVAL),
                users.countByRoleAndAccountStatus(UserRole.USER, AccountStatus.ACTIVE),
                users.countByRoleAndAccountStatus(UserRole.USER, AccountStatus.SUSPENDED),
                items.count(),
                items.countByApprovalStatus(ListingApprovalStatus.PENDING_APPROVAL),
                items.countByApprovalStatus(ListingApprovalStatus.APPROVED),
                items.countByApprovalStatus(ListingApprovalStatus.REJECTED),
                items.countByApprovalStatusAndAvailabilityStatus(ListingApprovalStatus.APPROVED, ItemAvailabilityStatus.AVAILABLE),
                items.countByAvailabilityStatus(ItemAvailabilityStatus.RESERVED),
                requests.countByStatus(ExchangeRequestStatus.COMPLETED),
                reports.countByStatusIn(AdminCountsService.OPEN_REPORTS),
                tickets.countByStatusIn(AdminCountsService.OPEN_TICKETS));

        LocalDate firstMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(WEEKS - 1L);
        LocalDateTime since = firstMonday.atStartOfDay();
        return new AdminDashboardView(kpis,
                weekly(users.registrationTimesSince(since), firstMonday),
                weekly(items.listingTimesSince(since), firstMonday),
                weekly(requests.completionTimesSince(since), firstMonday),
                audit.search(null, null, 0, 6).content());
    }

    /** Buckets timestamps into the last WEEKS ISO weeks, including empty weeks. */
    static List<AdminDashboardView.WeekPoint> weekly(List<LocalDateTime> times, LocalDate firstMonday) {
        Map<LocalDate, Long> buckets = new TreeMap<>();
        for (int i = 0; i < WEEKS; i++) {
            buckets.put(firstMonday.plusWeeks(i), 0L);
        }
        for (LocalDateTime t : times) {
            LocalDate monday = t.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            buckets.computeIfPresent(monday, (k, v) -> v + 1);
        }
        List<AdminDashboardView.WeekPoint> points = new ArrayList<>();
        buckets.forEach((week, count) -> points.add(new AdminDashboardView.WeekPoint(week.toString(), count)));
        return points;
    }
}
