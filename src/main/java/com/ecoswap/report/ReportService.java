package com.ecoswap.report;

import com.ecoswap.common.exception.BusinessRuleException;
import com.ecoswap.common.exception.InvalidInputException;
import com.ecoswap.common.exception.NotFoundException;
import com.ecoswap.common.util.Texts;
import com.ecoswap.item.entity.Item;
import com.ecoswap.item.repository.ItemRepository;
import com.ecoswap.notification.NotificationService;
import com.ecoswap.notification.NotificationType;
import com.ecoswap.user.entity.User;
import com.ecoswap.user.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Students report listings or other students; administrators handle them in AdminReportService. */
@Service
public class ReportService {

    private static final List<ReportStatus> OPEN = List.of(ReportStatus.OPEN, ReportStatus.UNDER_REVIEW);

    private final ReportRepository reports;
    private final ItemRepository items;
    private final UserRepository users;
    private final NotificationService notifications;

    public ReportService(ReportRepository reports, ItemRepository items, UserRepository users, NotificationService notifications) {
        this.reports = reports;
        this.items = items;
        this.users = users;
        this.notifications = notifications;
    }

    @Transactional
    public ReportView create(Long reporterId, CreateReportBody body) {
        User reporter = users.findById(reporterId).orElseThrow(() -> new NotFoundException("Account"));
        Item item = null;
        User reported;
        if (body.itemId() != null) {
            item = items.findById(body.itemId()).filter(Item::isPubliclyVisible).orElseThrow(() -> new NotFoundException("Listing"));
            reported = item.getOwner();
            if (reports.existsByReporterIdAndItemIdAndStatusIn(reporterId, item.getId(), OPEN)) {
                throw new BusinessRuleException("DUPLICATE_REPORT", "You've already reported this listing. The admin team is looking into it.");
            }
        } else if (body.reportedUserId() != null) {
            reported = users.findById(body.reportedUserId()).filter(u -> !u.isAdmin()).orElseThrow(() -> new NotFoundException("Student"));
            if (reports.existsByReporterIdAndReportedUserIdAndItemIsNullAndStatusIn(reporterId, reported.getId(), OPEN)) {
                throw new BusinessRuleException("DUPLICATE_REPORT", "You've already reported this student. The admin team is looking into it.");
            }
        } else {
            throw new InvalidInputException("itemId", "Choose what you are reporting.");
        }
        if (reported.getId().equals(reporterId)) {
            throw new BusinessRuleException("SELF_REPORT", "You can't report yourself or your own listing.");
        }
        Report report = reports.save(new Report(reporter, reported, item, body.reason(), Texts.clean(body.description())));
        notifications.notifyAdmins(NotificationType.REPORT_SUBMITTED, "New report: " + body.reason().label(),
                (item != null ? "Listing \"" + item.getName() + "\"" : "Student " + reported.getFullName()) + " was reported.",
                "/admin/reports");
        return ReportView.of(report);
    }
}
