package com.ecoswap.admin.service;

import com.ecoswap.admin.dto.AdminSidebarCounts;
import com.ecoswap.item.entity.ListingApprovalStatus;
import com.ecoswap.item.repository.ItemRepository;
import com.ecoswap.report.ReportRepository;
import com.ecoswap.report.ReportStatus;
import com.ecoswap.support.SupportTicketRepository;
import com.ecoswap.support.SupportTicketStatus;
import com.ecoswap.user.entity.AccountStatus;
import com.ecoswap.user.entity.UserRole;
import com.ecoswap.user.repository.UserRepository;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Work-queue counts shown on the admin sidebar. */
@Service
@Transactional(readOnly = true)
@PreAuthorize("hasRole('ADMIN')")
public class AdminCountsService {

    static final List<ReportStatus> OPEN_REPORTS = List.of(ReportStatus.OPEN, ReportStatus.UNDER_REVIEW);
    static final List<SupportTicketStatus> OPEN_TICKETS = List.of(SupportTicketStatus.OPEN, SupportTicketStatus.IN_PROGRESS);

    private final UserRepository users;
    private final ItemRepository items;
    private final ReportRepository reports;
    private final SupportTicketRepository tickets;

    public AdminCountsService(UserRepository users, ItemRepository items, ReportRepository reports, SupportTicketRepository tickets) {
        this.users = users;
        this.items = items;
        this.reports = reports;
        this.tickets = tickets;
    }

    public AdminSidebarCounts sidebarCounts() {
        return new AdminSidebarCounts(
                users.countByRoleAndAccountStatus(UserRole.USER, AccountStatus.PENDING_APPROVAL),
                items.countByApprovalStatus(ListingApprovalStatus.PENDING_APPROVAL),
                reports.countByStatusIn(OPEN_REPORTS),
                tickets.countByStatusIn(OPEN_TICKETS));
    }
}
