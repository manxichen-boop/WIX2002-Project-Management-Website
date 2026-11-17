package com.ecoswap.approval;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalHistoryRepository extends JpaRepository<ApprovalHistory, Long> {

    @EntityGraph(attributePaths = {"admin"})
    List<ApprovalHistory> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(ApprovalEntityType type, Long entityId);
}
