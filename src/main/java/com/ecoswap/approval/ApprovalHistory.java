package com.ecoswap.approval;

import com.ecoswap.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** One moderation decision on a registration or listing. Rows are never modified. */
@Entity
@Immutable
@Table(name = "approval_history")
public class ApprovalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "approval_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "entity_type", nullable = false, length = 20)
    private ApprovalEntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private ApprovalDecision decision;

    @Column(length = 500)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ApprovalHistory() {}

    public ApprovalHistory(ApprovalEntityType entityType, Long entityId, User admin, ApprovalDecision decision, String reason) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.admin = admin;
        this.decision = decision;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public ApprovalEntityType getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public User getAdmin() { return admin; }
    public ApprovalDecision getDecision() { return decision; }
    public String getReason() { return reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
