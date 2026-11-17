package com.ecoswap.audit;

import com.ecoswap.common.util.Times;
import java.time.Instant;

public record AuditLogView(Long id, Long adminId, String adminName, AuditAction action, AuditTargetType targetType,
                           Long targetId, String reason, Instant createdAt) {

    public static AuditLogView of(AdminAuditLog l) {
        return new AuditLogView(l.getId(), l.getAdmin().getId(), l.getAdmin().getFullName(), l.getAction(), l.getTargetType(),
                l.getTargetId(), l.getReason(), Times.utc(l.getCreatedAt()));
    }
}
