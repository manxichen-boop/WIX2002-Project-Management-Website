package com.ecoswap.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {

    @EntityGraph(attributePaths = {"admin"})
    @Query("""
            select l from AdminAuditLog l
            where (:action is null or l.action = :action)
              and (:targetType is null or l.targetType = :targetType)
            """)
    Page<AdminAuditLog> search(@Param("action") AuditAction action, @Param("targetType") AuditTargetType targetType,
                               Pageable pageable);
}
