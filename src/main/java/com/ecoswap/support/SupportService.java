package com.ecoswap.support;

import com.ecoswap.common.exception.BusinessRuleException;
import com.ecoswap.common.exception.NotFoundException;
import com.ecoswap.common.response.PageResponse;
import com.ecoswap.common.util.TokenGenerator;
import com.ecoswap.notification.NotificationService;
import com.ecoswap.notification.NotificationType;
import com.ecoswap.storage.FileCleanup;
import com.ecoswap.storage.StorageFolder;
import com.ecoswap.storage.StorageService;
import com.ecoswap.storage.UploadValidator;
import com.ecoswap.user.entity.User;
import com.ecoswap.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Student side of Help & Support. */
@Service
public class SupportService {

    private final SupportTicketRepository tickets;
    private final UserRepository users;
    private final UploadValidator uploads;
    private final StorageService storage;
    private final FileCleanup cleanup;
    private final NotificationService notifications;

    public SupportService(SupportTicketRepository tickets, UserRepository users, UploadValidator uploads, StorageService storage,
                          FileCleanup cleanup, NotificationService notifications) {
        this.tickets = tickets;
        this.users = users;
        this.uploads = uploads;
        this.storage = storage;
        this.cleanup = cleanup;
        this.notifications = notifications;
    }

    @Transactional
    public SupportTicketView create(Long userId, CreateTicketForm form, MultipartFile attachment) {
        User user = users.findById(userId).orElseThrow(() -> new NotFoundException("Account"));
        SupportTicket ticket = new SupportTicket(newReference(), user, form.subject().strip(), form.message().strip());
        if (attachment != null && !attachment.isEmpty()) {
            var upload = uploads.attachment(attachment, "attachment");
            var stored = storage.store(StorageFolder.SUPPORT, upload.content(), upload.type(), upload.originalFilename());
            cleanup.deleteOnRollback(new ArrayList<>(List.of(stored.key())));
            ticket.attach(stored.key(), stored.originalFilename(), stored.mimeType());
        }
        tickets.save(ticket);
        notifications.notifyAdmins(NotificationType.SUPPORT_TICKET_CREATED, "New support ticket " + ticket.getReferenceCode(),
                user.getFullName() + ": " + ticket.getSubject(), "/admin/support");
        return SupportTicketView.detail(ticket, false);
    }

    @Transactional(readOnly = true)
    public PageResponse<SupportTicketView> mine(Long userId, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by(Sort.Direction.DESC, "updatedAt"));
        return PageResponse.of(tickets.findByUserId(userId, pageable), t -> SupportTicketView.summary(t, false));
    }

    @Transactional(readOnly = true)
    public SupportTicketView detail(Long userId, Long ticketId) {
        return SupportTicketView.detail(owned(userId, ticketId), false);
    }

    @Transactional
    public SupportTicketView reply(Long userId, Long ticketId, String message) {
        SupportTicket ticket = owned(userId, ticketId);
        if (ticket.getStatus() == SupportTicketStatus.CLOSED) {
            throw new BusinessRuleException("TICKET_CLOSED", "This ticket is closed. Please open a new one.");
        }
        ticket.addReply(ticket.getUser(), message.strip());
        ticket.changeStatus(SupportTicketStatus.OPEN);   // a student reply puts it back in the team's queue
        notifications.notifyAdmins(NotificationType.SUPPORT_REPLY, "Student replied on " + ticket.getReferenceCode(),
                ticket.getUser().getFullName() + ": " + ticket.getSubject(), "/admin/support");
        return SupportTicketView.detail(ticket, false);
    }

    private SupportTicket owned(Long userId, Long ticketId) {
        return tickets.findDetailed(ticketId).filter(t -> t.getUser().getId().equals(userId))
                .orElseThrow(() -> new NotFoundException("Ticket"));
    }

    private String newReference() {
        String code;
        do {
            code = "ES-" + TokenGenerator.shortCode(6);
        } while (tickets.existsByReferenceCode(code));
        return code;
    }
}
