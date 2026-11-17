package com.ecoswap.exchange.entity;

/**
 * PENDING → ACCEPTED → COMPLETED is the happy path.
 * REJECTED (by owner), CANCELLED (by a participant) and CLOSED (automatically, because the item
 * was reserved through another request) are terminal.
 */
public enum ExchangeRequestStatus {
    PENDING, ACCEPTED, REJECTED, CANCELLED, CLOSED, COMPLETED;

    public boolean isActive() { return this == PENDING || this == ACCEPTED; }
}
