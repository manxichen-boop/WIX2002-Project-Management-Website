package com.ecoswap.support;

import com.ecoswap.common.util.Times;
import java.time.Instant;
import java.util.List;

/** Ticket with its conversation. studentEmail is only filled for administrators. */
public record SupportTicketView(Long id, String referenceCode, String subject, String message, SupportTicketStatus status,
                                Instant createdAt, Instant updatedAt, String studentName, String studentEmail,
                                String attachmentUrl, String attachmentName, int replyCount, List<Reply> replies) {

    public record Reply(Long id, String senderName, boolean staff, String message, Instant createdAt) {}

    public static SupportTicketView summary(SupportTicket t, boolean forAdmin) {
        return of(t, forAdmin, false);
    }

    public static SupportTicketView detail(SupportTicket t, boolean forAdmin) {
        return of(t, forAdmin, true);
    }

    private static SupportTicketView of(SupportTicket t, boolean forAdmin, boolean withReplies) {
        List<Reply> replies = withReplies ? t.getReplies().stream()
                .map(r -> new Reply(r.getId(), r.getSender().isAdmin() ? "Eco Swape team" : r.getSender().getFullName(),
                        r.getSender().isAdmin(), r.getMessage(), Times.utc(r.getCreatedAt())))
                .toList() : List.of();
        return new SupportTicketView(t.getId(), t.getReferenceCode(), t.getSubject(), withReplies ? t.getMessage() : null, t.getStatus(),
                Times.utc(t.getCreatedAt()), Times.utc(t.getUpdatedAt()), t.getUser().getFullName(),
                forAdmin ? t.getUser().getEmail() : null,
                t.getAttachmentPath() == null ? null : "/media/support/" + t.getId(), t.getAttachmentOriginalFilename(),
                withReplies ? replies.size() : 0, replies);
    }
}
