package com.ecoswap.admin.service;

import com.ecoswap.admin.dto.AdminUserView;
import com.ecoswap.approval.ApprovalDecision;
import com.ecoswap.approval.ApprovalEntityType;
import com.ecoswap.approval.ApprovalService;
import com.ecoswap.audit.AuditAction;
import com.ecoswap.audit.AuditService;
import com.ecoswap.audit.AuditTargetType;
import com.ecoswap.auth.service.MailService;
import com.ecoswap.common.exception.BusinessRuleException;
import com.ecoswap.common.exception.InvalidInputException;
import com.ecoswap.common.exception.NotFoundException;
import com.ecoswap.common.response.PageResponse;
import com.ecoswap.common.util.Texts;
import com.ecoswap.config.AppProperties;
import com.ecoswap.notification.NotificationService;
import com.ecoswap.notification.NotificationType;
import com.ecoswap.user.entity.AccountStatus;
import com.ecoswap.user.entity.User;
import com.ecoswap.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registration approval and account moderation. Administrator accounts can't be moderated here. */
@Service
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserService {

    private final UserRepository users;
    private final ApprovalService approvals;
    private final AuditService audit;
    private final NotificationService notifications;
    private final MailService mail;
    private final AppProperties props;

    public AdminUserService(UserRepository users, ApprovalService approvals, AuditService audit,
                            NotificationService notifications, MailService mail, AppProperties props) {
        this.users = users;
        this.approvals = approvals;
        this.audit = audit;
        this.notifications = notifications;
        this.mail = mail;
        this.props = props;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserView> search(AccountStatus status, String q, int page, int size) {
        String like = q == null || q.isBlank() ? null : Texts.likePattern(q);
        Sort sort = status == AccountStatus.PENDING_APPROVAL ? Sort.by("createdAt") : Sort.by(Sort.Direction.DESC, "createdAt");
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), sort.and(Sort.by("id")));
        return PageResponse.of(users.searchStudents(status, like, pageable), AdminUserView::of);
    }

    @Transactional
    public AdminUserView approve(Long adminId, Long userId) {
        User admin = admin(adminId);
        User user = student(userId);
        requireStatus(user, AccountStatus.PENDING_APPROVAL, "Only pending registrations can be approved.");
        if (props.auth().requireEmailVerification() && !user.isEmailVerified()) {
            throw new BusinessRuleException("EMAIL_NOT_VERIFIED", "This student hasn't confirmed their e-mail address yet.");
        }
        user.changeStatus(AccountStatus.ACTIVE, null);
        record(admin, user, ApprovalDecision.APPROVED, AuditAction.REGISTRATION_APPROVED, null);
        notifications.notify(user, NotificationType.REGISTRATION_APPROVED, "Welcome to Eco Swape!",
                "Your account has been approved. You can now post items and request exchanges.", "/dashboard");
        mail.send(user.getEmail(), "Your Eco Swape account is approved",
                "Hi " + user.getFullName() + ",\n\nGood news: your Eco Swape account has been approved. Log in here:\n\n"
                        + mail.link("/login") + "\n\nHappy swapping!\nEco Swape");
        return AdminUserView.of(user);
    }

    @Transactional
    public AdminUserView reject(Long adminId, Long userId, String reason) {
        User admin = admin(adminId);
        User user = student(userId);
        requireStatus(user, AccountStatus.PENDING_APPROVAL, "Only pending registrations can be rejected.");
        String why = requireReason(reason);
        user.changeStatus(AccountStatus.REJECTED, why);
        record(admin, user, ApprovalDecision.REJECTED, AuditAction.REGISTRATION_REJECTED, why);
        notifications.notify(user, NotificationType.REGISTRATION_REJECTED, "Registration not approved", "Reason: " + why, null);
        mail.send(user.getEmail(), "About your Eco Swape registration",
                "Hi " + user.getFullName() + ",\n\nUnfortunately your Eco Swape registration was not approved.\nReason: " + why
                        + "\n\nIf you think this is a mistake, reply to this e-mail.\n\nEco Swape");
        return AdminUserView.of(user);
    }

    @Transactional
    public AdminUserView suspend(Long adminId, Long userId, String reason) {
        User admin = admin(adminId);
        User user = student(userId);
        requireStatus(user, AccountStatus.ACTIVE, "Only active accounts can be suspended.");
        String why = requireReason(reason);
        user.changeStatus(AccountStatus.SUSPENDED, why);   // AccountStatusFilter ends their sessions on the next request
        record(admin, user, ApprovalDecision.SUSPENDED, AuditAction.ACCOUNT_SUSPENDED, why);
        notifications.notify(user, NotificationType.ACCOUNT_SUSPENDED, "Account suspended", "Reason: " + why, null);
        mail.send(user.getEmail(), "Your Eco Swape account has been suspended",
                "Hi " + user.getFullName() + ",\n\nYour Eco Swape account has been suspended.\nReason: " + why + "\n\nEco Swape");
        return AdminUserView.of(user);
    }

    @Transactional
    public AdminUserView activate(Long adminId, Long userId) {
        User admin = admin(adminId);
        User user = student(userId);
        if (user.getAccountStatus() != AccountStatus.SUSPENDED && user.getAccountStatus() != AccountStatus.REJECTED) {
            throw new BusinessRuleException("INVALID_STATE", "Only suspended or rejected accounts can be reactivated.");
        }
        user.changeStatus(AccountStatus.ACTIVE, null);
        record(admin, user, ApprovalDecision.REACTIVATED, AuditAction.ACCOUNT_REACTIVATED, null);
        notifications.notify(user, NotificationType.ACCOUNT_REACTIVATED, "Account reactivated",
                "Your Eco Swape account is active again.", "/dashboard");
        mail.send(user.getEmail(), "Your Eco Swape account is active again",
                "Hi " + user.getFullName() + ",\n\nYour account has been reactivated. Log in: " + mail.link("/login") + "\n\nEco Swape");
        return AdminUserView.of(user);
    }

    private void record(User admin, User user, ApprovalDecision decision, AuditAction action, String reason) {
        approvals.record(ApprovalEntityType.USER, user.getId(), admin, decision, reason);
        audit.log(admin, action, AuditTargetType.USER, user.getId(), reason);
    }

    private User admin(Long adminId) {
        return users.findById(adminId).orElseThrow(() -> new NotFoundException("Administrator"));
    }

    private User student(Long userId) {
        return users.findById(userId).filter(u -> !u.isAdmin()).orElseThrow(() -> new NotFoundException("Student account"));
    }

    private static void requireStatus(User user, AccountStatus expected, String message) {
        if (user.getAccountStatus() != expected) {
            throw new BusinessRuleException("INVALID_STATE", message);
        }
    }

    static String requireReason(String reason) {
        String why = Texts.clean(reason);
        if (why == null || why.length() < 3) {
            throw new InvalidInputException("reason", "Please give a short reason (it is shown to the student).");
        }
        return why;
    }
}
