package com.ecoswap.admin.service;

import com.ecoswap.audit.AuditAction;
import com.ecoswap.audit.AuditService;
import com.ecoswap.audit.AuditTargetType;
import com.ecoswap.common.exception.BusinessRuleException;
import com.ecoswap.common.exception.NotFoundException;
import com.ecoswap.common.response.PageResponse;
import com.ecoswap.item.entity.ListingApprovalStatus;
import com.ecoswap.report.Report;
import com.ecoswap.report.ReportRepository;
import com.ecoswap.report.ReportStatus;
import com.ecoswap.report.ReportView;
import com.ecoswap.user.entity.AccountStatus;
import com.ecoswap.user.entity.User;
import com.ecoswap.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Report handling. Resolving can optionally suspend the reported listing and/or student; those
 * actions reuse the listing and user moderation services so they get the same history, audit
 * entries and notifications as any other moderation.
 */
@Service
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportService {

    private final ReportRepository reports;
    private final UserRepository users;
    private final AuditService audit;
    private final AdminListingService listings;
    private final AdminUserService userModeration;

    public AdminReportService(ReportRepository reports, UserRepository users, AuditService audit,
                              AdminListingService listings, AdminUserService userModeration) {
        this.reports = reports;
        this.users = users;
        this.audit = audit;
        this.listings = listings;
        this.userModeration = userModeration;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportView> search(ReportStatus status, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return PageResponse.of(reports.search(status, pageable), ReportView::of);
    }

    @Transactional
    public ReportView startReview(Long adminId, Long reportId) {
        User admin = admin(adminId);
        Report report = open(reportId);
        report.startReview(admin);
        audit.log(admin, AuditAction.REPORT_REVIEW_STARTED, AuditTargetType.REPORT, report.getId(), null);
        return ReportView.of(report);
    }

    @Transactional
    public ReportView resolve(Long adminId, Long reportId, String notes, boolean suspendListing, boolean suspendUser) {
        User admin = admin(adminId);
        Report report = open(reportId);
        String why = AdminUserService.requireReason(notes);
        String reason = "Report #" + report.getId() + " (" + report.getReason().label() + "): " + why;
        if (suspendListing && report.getItem() != null && report.getItem().getApprovalStatus() != ListingApprovalStatus.SUSPENDED) {
            listings.suspend(adminId, report.getItem().getId(), reason);
        }
        if (suspendUser && report.getReportedUser() != null && report.getReportedUser().getAccountStatus() == AccountStatus.ACTIVE) {
            userModeration.suspend(adminId, report.getReportedUser().getId(), reason);
        }
        report.close(ReportStatus.RESOLVED, admin, why);
        audit.log(admin, AuditAction.REPORT_RESOLVED, AuditTargetType.REPORT, report.getId(), why);
        return ReportView.of(report);
    }

    @Transactional
    public ReportView dismiss(Long adminId, Long reportId, String notes) {
        User admin = admin(adminId);
        Report report = open(reportId);
        String why = AdminUserService.requireReason(notes);
        report.close(ReportStatus.DISMISSED, admin, why);
        audit.log(admin, AuditAction.REPORT_DISMISSED, AuditTargetType.REPORT, report.getId(), why);
        return ReportView.of(report);
    }

    private Report open(Long id) {
        Report report = reports.findById(id).orElseThrow(() -> new NotFoundException("Report"));
        if (report.isClosed()) {
            throw new BusinessRuleException("INVALID_STATE", "This report has already been closed.");
        }
        return report;
    }

    private User admin(Long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("Administrator"));
    }
}
