package com.ecoswap.exchange.repository;

import com.ecoswap.exchange.entity.ExchangeRequest;
import com.ecoswap.exchange.entity.ExchangeRequestStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExchangeRequestRepository extends JpaRepository<ExchangeRequest, Long> {

    String GRAPH_ITEM = "item";

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ExchangeRequest r where r.id = :id")
    Optional<ExchangeRequest> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"item", "item.owner", "item.category", "requester", "offeredItem"})
    @Query("select r from ExchangeRequest r where r.id = :id")
    Optional<ExchangeRequest> findDetailed(@Param("id") Long id);

    /** Requests whose chat the user can open: accepted/completed ones, plus closed ones that already have messages. */
    @EntityGraph(attributePaths = {"item", "item.owner", "item.category", "requester", "offeredItem"})
    @Query("""
            select r from ExchangeRequest r
            where (r.requester.id = :userId or r.item.owner.id = :userId)
              and (r.status in :open or exists (select m.id from com.ecoswap.chat.ChatMessage m where m.request = r))
            order by r.updatedAt desc
            """)
    List<ExchangeRequest> findConversations(@Param("userId") Long userId, @Param("open") Collection<ExchangeRequestStatus> open,
                                            Pageable pageable);

    Optional<ExchangeRequest> findFirstByItemIdAndRequesterIdAndStatusIn(Long itemId, Long requesterId, Collection<ExchangeRequestStatus> statuses);

    boolean existsByItemIdAndRequesterIdAndStatusIn(Long itemId, Long requesterId, Collection<ExchangeRequestStatus> statuses);

    /** Pending requests that can no longer succeed once {@code itemId} is reserved (as target or as offer). */
    @Query("""
            select r from ExchangeRequest r
            where r.status = com.ecoswap.exchange.entity.ExchangeRequestStatus.PENDING
              and r.id <> :exceptId
              and (r.item.id in :itemIds or r.offeredItem.id in :itemIds)
            """)
    List<ExchangeRequest> findPendingInvolving(@Param("itemIds") Collection<Long> itemIds, @Param("exceptId") Long exceptId);

    @Query("""
            select r from ExchangeRequest r
            where r.status = com.ecoswap.exchange.entity.ExchangeRequestStatus.PENDING
              and (r.item.id = :itemId or r.offeredItem.id = :itemId)
            """)
    List<ExchangeRequest> findPendingInvolvingItem(@Param("itemId") Long itemId);

    @Query("""
            select r from ExchangeRequest r
            where r.status = com.ecoswap.exchange.entity.ExchangeRequestStatus.ACCEPTED
              and (r.item.id = :itemId or r.offeredItem.id = :itemId)
            """)
    List<ExchangeRequest> findAcceptedInvolvingItem(@Param("itemId") Long itemId);

    long countByItemIdAndStatus(Long itemId, ExchangeRequestStatus status);

    @Query("select r.item.id, count(r) from ExchangeRequest r where r.item.id in :itemIds and r.status = :status group by r.item.id")
    List<Object[]> countByItemIdsAndStatus(@Param("itemIds") Collection<Long> itemIds, @Param("status") ExchangeRequestStatus status);

    @EntityGraph(attributePaths = {"item", "item.owner", "offeredItem"})
    @Query("""
            select r from ExchangeRequest r
            where r.requester.id = :userId and (:status is null or r.status = :status)
            """)
    Page<ExchangeRequest> findSent(@Param("userId") Long userId, @Param("status") ExchangeRequestStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"item", "requester", "offeredItem"})
    @Query("""
            select r from ExchangeRequest r
            where r.item.owner.id = :userId and (:status is null or r.status = :status)
            """)
    Page<ExchangeRequest> findReceived(@Param("userId") Long userId, @Param("status") ExchangeRequestStatus status, Pageable pageable);

    /** Accepted and completed exchanges the user took part in, either side. */
    @EntityGraph(attributePaths = {"item", "item.owner", "requester", "offeredItem"})
    @Query("""
            select r from ExchangeRequest r
            where (r.requester.id = :userId or r.item.owner.id = :userId)
              and r.status in :statuses
            """)
    Page<ExchangeRequest> findHistory(@Param("userId") Long userId,
                                      @Param("statuses") Collection<ExchangeRequestStatus> statuses, Pageable pageable);

    long countByRequesterIdAndStatus(Long requesterId, ExchangeRequestStatus status);

    long countByItemOwnerIdAndStatus(Long ownerId, ExchangeRequestStatus status);

    @Query("""
            select count(r) from ExchangeRequest r
            where (r.requester.id = :userId or r.item.owner.id = :userId) and r.status = :status
            """)
    long countParticipating(@Param("userId") Long userId, @Param("status") ExchangeRequestStatus status);

    long countByStatus(ExchangeRequestStatus status);

    @EntityGraph(attributePaths = {"item", "item.owner", "requester", "offeredItem"})
    @Query("""
            select r from ExchangeRequest r
            where (:status is null or r.status = :status)
              and (:q is null or lower(r.item.name) like :q or lower(r.requester.fullName) like :q
                   or lower(r.item.owner.fullName) like :q)
            """)
    Page<ExchangeRequest> searchAll(@Param("status") ExchangeRequestStatus status, @Param("q") String likePattern, Pageable pageable);

    @EntityGraph(attributePaths = {"item", "item.category"})
    List<ExchangeRequest> findTop6ByStatusOrderByCompletedAtDesc(ExchangeRequestStatus status);

    @Query("select r.completedAt from ExchangeRequest r where r.status = com.ecoswap.exchange.entity.ExchangeRequestStatus.COMPLETED and r.completedAt >= :since")
    List<LocalDateTime> completionTimesSince(@Param("since") LocalDateTime since);
}
