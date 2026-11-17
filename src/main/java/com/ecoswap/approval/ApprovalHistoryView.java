package com.ecoswap.approval;

import com.ecoswap.common.util.Times;
import java.time.Instant;

public record ApprovalHistoryView(Long id, ApprovalDecision decision, String reason, String adminName, Instant createdAt) {

    public static ApprovalHistoryView of(ApprovalHistory h) {
        return new ApprovalHistoryView(h.getId(), h.getDecision(), h.getReason(), h.getAdmin().getFullName(), Times.utc(h.getCreatedAt()));
    }
}
