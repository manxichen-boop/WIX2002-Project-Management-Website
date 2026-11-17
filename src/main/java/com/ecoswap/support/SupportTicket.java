package com.ecoswap.support;

import com.ecoswap.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "support_tickets")
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Long id;

    @Column(name = "reference_code", nullable = false, length = 20, unique = true)
    private String referenceCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 150)
    private String subject;

    @Column(nullable = false, length = 4000)
    private String message;

    @Column(name = "attachment_path", length = 255)
    private String attachmentPath;

    @Column(name = "attachment_original_filename", length = 255)
    private String attachmentOriginalFilename;

    @Column(name = "attachment_mime_type", length = 50)
    private String attachmentMimeType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private SupportTicketStatus status = SupportTicketStatus.OPEN;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<SupportReply> replies = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected SupportTicket() {}

    public SupportTicket(String referenceCode, User user, String subject, String message) {
        this.referenceCode = referenceCode;
        this.user = user;
        this.subject = subject;
        this.message = message;
    }

    public void attach(String path, String originalFilename, String mimeType) {
        this.attachmentPath = path;
        this.attachmentOriginalFilename = originalFilename;
        this.attachmentMimeType = mimeType;
    }

    public SupportReply addReply(User sender, String message) {
        SupportReply reply = new SupportReply(this, sender, message);
        replies.add(reply);
        return reply;
    }

    public void changeStatus(SupportTicketStatus status) { this.status = status; }

    public Long getId() { return id; }
    public String getReferenceCode() { return referenceCode; }
    public User getUser() { return user; }
    public String getSubject() { return subject; }
    public String getMessage() { return message; }
    public String getAttachmentPath() { return attachmentPath; }
    public String getAttachmentOriginalFilename() { return attachmentOriginalFilename; }
    public String getAttachmentMimeType() { return attachmentMimeType; }
    public SupportTicketStatus getStatus() { return status; }
    public List<SupportReply> getReplies() { return replies; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
