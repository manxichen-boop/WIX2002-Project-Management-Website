package com.ecoswap.support;

import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    Page<SupportTicket> findByUserId(Long userId, Pageable pageable);

    boolean existsByReferenceCode(String referenceCode);

    @EntityGraph(attributePaths = {"user", "replies", "replies.sender"})
    @Query("select t from SupportTicket t where t.id = :id")
    Optional<SupportTicket> findDetailed(@Param("id") Long id);

    @EntityGraph(attributePaths = {"user"})
    @Query("""
            select t from SupportTicket t
            where (:status is null or t.status = :status)
              and (:q is null or lower(t.subject) like :q or lower(t.referenceCode) like :q
                   or lower(t.user.fullName) like :q or lower(t.user.email) like :q)
            """)
    Page<SupportTicket> search(@Param("status") SupportTicketStatus status, @Param("q") String likePattern, Pageable pageable);

    long countByStatusIn(Collection<SupportTicketStatus> statuses);
}
