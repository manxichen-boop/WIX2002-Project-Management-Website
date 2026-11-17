package com.ecoswap.exchange.entity;

import com.ecoswap.item.entity.Item;
import com.ecoswap.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "exchange_requests")
public class ExchangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offered_item_id")
    private Item offeredItem;

    @Column(length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private ExchangeRequestStatus status = ExchangeRequestStatus.PENDING;

    @Column(name = "status_reason", length = 500)
    private String statusReason;

    @Column(name = "owner_confirmed_completion", nullable = false)
    private boolean ownerConfirmedCompletion;

    @Column(name = "requester_confirmed_completion", nullable = false)
    private boolean requesterConfirmedCompletion;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected ExchangeRequest() {}

    public ExchangeRequest(Item item, User requester, Item offeredItem, String message) {
        this.item = item;
        this.requester = requester;
        this.offeredItem = offeredItem;
        this.message = message;
    }

    public boolean isOwner(Long userId) { return item.getOwner().getId().equals(userId); }
    public boolean isRequester(Long userId) { return requester.getId().equals(userId); }
    public boolean isParticipant(Long userId) { return isOwner(userId) || isRequester(userId); }

    public void accept() { this.status = ExchangeRequestStatus.ACCEPTED; }

    public void finish(ExchangeRequestStatus terminal, String reason) {
        this.status = terminal;
        this.statusReason = reason;
    }

    /** Records one participant's confirmation; returns true once both have confirmed. */
    public boolean confirmCompletionBy(Long userId) {
        if (isOwner(userId)) {
            ownerConfirmedCompletion = true;
        }
        if (isRequester(userId)) {
            requesterConfirmedCompletion = true;
        }
        if (ownerConfirmedCompletion && requesterConfirmedCompletion) {
            status = ExchangeRequestStatus.COMPLETED;
            completedAt = LocalDateTime.now();
            return true;
        }
        return false;
    }

    public Long getId() { return id; }
    public Item getItem() { return item; }
    public User getRequester() { return requester; }
    public Item getOfferedItem() { return offeredItem; }
    public String getMessage() { return message; }
    public ExchangeRequestStatus getStatus() { return status; }
    public String getStatusReason() { return statusReason; }
    public boolean isOwnerConfirmedCompletion() { return ownerConfirmedCompletion; }
    public boolean isRequesterConfirmedCompletion() { return requesterConfirmedCompletion; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
