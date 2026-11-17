package com.ecoswap.report;

import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, Long> {

    @EntityGraph(attributePaths = {"reporter", "reportedUser", "item", "reviewedBy"})
    @Query("select r from Report r where (:status is null or r.status = :status)")
    Page<Report> search(@Param("status") ReportStatus status, Pageable pageable);

    long countByStatusIn(Collection<ReportStatus> statuses);

    boolean existsByReporterIdAndItemIdAndStatusIn(Long reporterId, Long itemId, Collection<ReportStatus> statuses);

    boolean existsByReporterIdAndReportedUserIdAndItemIsNullAndStatusIn(Long reporterId, Long reportedUserId,
                                                                         Collection<ReportStatus> statuses);
}
