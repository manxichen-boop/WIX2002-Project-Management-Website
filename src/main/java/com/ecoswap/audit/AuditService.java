package com.ecoswap.audit;

import com.ecoswap.common.response.PageResponse;
import com.ecoswap.user.entity.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Append-only admin audit trail. There is intentionally no update or delete operation. */
@Service
public class AuditService {

    private final AdminAuditLogRepository logs;

    public AuditService(AdminAuditLogRepository logs) {
        this.logs = logs;
    }

    /** Written in the same transaction as the action, so an action and its audit row succeed or fail together. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void log(User admin, AuditAction action, AuditTargetType targetType, Long targetId, String reason) {
        logs.save(new AdminAuditLog(admin, action, targetType, targetId, reason));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public PageResponse<AuditLogView> search(AuditAction action, AuditTargetType targetType, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return PageResponse.of(logs.search(action, targetType, pageable), AuditLogView::of);
    }
}
