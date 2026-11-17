package com.ecoswap.chat;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /** Newest first; the service reverses the page for display. */
    @EntityGraph(attributePaths = "sender")
    List<ChatMessage> findByRequestIdOrderByIdDesc(Long requestId, Pageable pageable);

    @EntityGraph(attributePaths = "sender")
    List<ChatMessage> findByRequestIdAndIdLessThanOrderByIdDesc(Long requestId, Long beforeId, Pageable pageable);

    @EntityGraph(attributePaths = "sender")
    List<ChatMessage> findByRequestIdAndIdGreaterThanOrderByIdAsc(Long requestId, Long afterId, Pageable pageable);

    boolean existsByRequestIdAndIdLessThan(Long requestId, Long id);

    /** Marks everything the other participant sent as read. */
    @Modifying(flushAutomatically = true, clearAutomatically = false)
    @Query("update ChatMessage m set m.readAt = :now where m.request.id = :requestId and m.sender.id <> :readerId and m.readAt is null")
    int markRead(@Param("requestId") Long requestId, @Param("readerId") Long readerId, @Param("now") LocalDateTime now);

    @Query("""
            select count(m) from ChatMessage m
            where m.readAt is null and m.sender.id <> :userId
              and (m.request.requester.id = :userId or m.request.item.owner.id = :userId)
            """)
    long countUnreadFor(@Param("userId") Long userId);

    @Query("""
            select m.request.id, count(m) from ChatMessage m
            where m.request.id in :requestIds and m.readAt is null and m.sender.id <> :userId
            group by m.request.id
            """)
    List<Object[]> countUnreadByRequest(@Param("requestIds") Collection<Long> requestIds, @Param("userId") Long userId);

    @EntityGraph(attributePaths = "sender")
    @Query("""
            select m from ChatMessage m where m.id in (
                select max(m2.id) from ChatMessage m2 where m2.request.id in :requestIds group by m2.request.id)
            """)
    List<ChatMessage> findLatestPerRequest(@Param("requestIds") Collection<Long> requestIds);

    /** Unread messages this sender already has waiting in the thread (used to avoid notification spam). */
    long countByRequestIdAndSenderIdAndReadAtIsNull(Long requestId, Long senderId);

    long countBySenderIdAndCreatedAtAfter(Long senderId, LocalDateTime since);
}
