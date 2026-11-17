package com.ecoswap.admin.service;

import com.ecoswap.audit.AuditAction;
import com.ecoswap.audit.AuditService;
import com.ecoswap.audit.AuditTargetType;
import com.ecoswap.common.exception.BusinessRuleException;
import com.ecoswap.common.exception.NotFoundException;
import com.ecoswap.common.response.PageResponse;
import com.ecoswap.common.util.Texts;
import com.ecoswap.notification.NotificationService;
import com.ecoswap.notification.NotificationType;
import com.ecoswap.support.SupportTicket;
import com.ecoswap.support.SupportTicketRepository;
import com.ecoswap.support.SupportTicketStatus;
import com.ecoswap.support.SupportTicketView;
import com.ecoswap.user.entity.User;
import com.ecoswap.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class AdminSupportService {

    private final SupportTicketRepository tickets;
    private final UserRepository users;
    private final AuditService audit;
    private final NotificationService notifications;

    public AdminSupportService(SupportTicketRepository tickets, UserRepository users, AuditService audit, NotificationService notifications) {
        this.tickets = tickets;
        this.users = users;
        this.audit = audit;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public PageResponse<SupportTicketView> search(SupportTicketStatus status, String q, int page, int size) {
        String like = q == null || q.isBlank() ? null : Texts.likePattern(q);
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by(Sort.Direction.DESC, "updatedAt", "id"));
        return PageResponse.of(tickets.search(status, like, pageable), t -> SupportTicketView.summary(t, true));
    }

    @Transactional(readOnly = true)
    public SupportTicketView detail(Long ticketId) {
        return SupportTicketView.detail(find(ticketId), true);
    }

    /** Replying moves an OPEN ticket to WAITING_FOR_USER unless the admin chooses another status. */
    @Transactional
    public SupportTicketView reply(Long adminId, Long ticketId, String message) {
        User admin = users.findById(adminId).orElseThrow(() -> new NotFoundException("Administrator"));
        SupportTicket ticket = find(ticketId);
        if (ticket.getStatus() == SupportTicketStatus.CLOSED) {
            throw new BusinessRuleException("TICKET_CLOSED", "Reopen the ticket before replying.");
        }
        ticket.addReply(admin, message.strip());
        if (ticket.getStatus() == SupportTicketStatus.OPEN || ticket.getStatus() == SupportTicketStatus.IN_PROGRESS) {
            ticket.changeStatus(SupportTicketStatus.WAITING_FOR_USER);
        }
        audit.log(admin, AuditAction.SUPPORT_REPLIED, AuditTargetType.SUPPORT_TICKET, ticket.getId(), null);
        notifications.notify(ticket.getUser(), NotificationType.SUPPORT_REPLY, "Reply on " + ticket.getReferenceCode(),
                "The Eco Swape team replied to \"" + ticket.getSubject() + "\".", "/support?ticket=" + ticket.getId());
        return SupportTicketView.detail(ticket, true);
    }

    @Transactional
    public SupportTicketView changeStatus(Long adminId, Long ticketId, SupportTicketStatus status) {
        User admin = users.findById(adminId).orElseThrow(() -> new NotFoundException("Administrator"));
        SupportTicket ticket = find(ticketId);
        if (ticket.getStatus() == status) {
            return SupportTicketView.detail(ticket, true);
        }
        ticket.changeStatus(status);
        audit.log(admin, AuditAction.SUPPORT_STATUS_CHANGED, AuditTargetType.SUPPORT_TICKET, ticket.getId(), "Status: " + status);
        notifications.notify(ticket.getUser(), NotificationType.SUPPORT_STATUS_CHANGED, "Ticket " + ticket.getReferenceCode() + " updated",
                "Status changed to " + status.name().replace('_', ' ').toLowerCase() + ".", "/support?ticket=" + ticket.getId());
        return SupportTicketView.detail(ticket, true);
    }

    private SupportTicket find(Long id) {
        return tickets.findDetailed(id).orElseThrow(() -> new NotFoundException("Ticket"));
    }
}
