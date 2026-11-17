package com.ecoswap.item.repository;

import com.ecoswap.item.entity.Item;
import com.ecoswap.item.entity.ItemAvailabilityStatus;
import com.ecoswap.item.entity.ListingApprovalStatus;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

    /** Row lock used when reserving items, so two acceptances can never reserve the same item. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Item i where i.id = :id")
    Optional<Item> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"owner", "category"})
    @Query("select i from Item i where i.id = :id")
    Optional<Item> findWithOwnerAndCategory(@Param("id") Long id);

    @EntityGraph(attributePaths = {"category"})
    Page<Item> findByOwnerIdAndAvailabilityStatusNot(Long ownerId, ItemAvailabilityStatus excluded, Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    Page<Item> findByOwnerIdAndAvailabilityStatus(Long ownerId, ItemAvailabilityStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    List<Item> findByOwnerIdAndApprovalStatusAndAvailabilityStatusOrderByCreatedAtDesc(
            Long ownerId, ListingApprovalStatus approval, ItemAvailabilityStatus availability);

    @EntityGraph(attributePaths = {"category", "owner"})
    List<Item> findTop8ByApprovalStatusAndAvailabilityStatusOrderByCreatedAtDesc(
            ListingApprovalStatus approval, ItemAvailabilityStatus availability);

    long countByApprovalStatus(ListingApprovalStatus status);

    long countByApprovalStatusAndAvailabilityStatus(ListingApprovalStatus approval, ItemAvailabilityStatus availability);

    long countByAvailabilityStatus(ItemAvailabilityStatus status);

    long countByOwnerIdAndApprovalStatusAndAvailabilityStatus(Long ownerId, ListingApprovalStatus approval,
                                                              ItemAvailabilityStatus availability);

    long countByOwnerIdAndApprovalStatusIn(Long ownerId, List<ListingApprovalStatus> statuses);

    /** Total recorded weight (kg) of items that changed hands through a completed exchange. */
    @Query("select coalesce(sum(i.approximateWeight), 0) from Item i where i.availabilityStatus = com.ecoswap.item.entity.ItemAvailabilityStatus.COMPLETED")
    BigDecimal sumReusedWeight();

    @Query("select count(i) from Item i where i.availabilityStatus = com.ecoswap.item.entity.ItemAvailabilityStatus.COMPLETED and i.approximateWeight is not null")
    long countReusedWithWeight();

    @Query("""
            select i.listingType, count(i) from Item i
            where i.availabilityStatus = com.ecoswap.item.entity.ItemAvailabilityStatus.COMPLETED
            group by i.listingType
            """)
    List<Object[]> countReusedByListingType();

    @Query("""
            select i.category.name, count(i) from Item i
            where i.availabilityStatus = com.ecoswap.item.entity.ItemAvailabilityStatus.COMPLETED
            group by i.category.name order by count(i) desc
            """)
    List<Object[]> countReusedByCategory();

    /** Approved, available listings that have a map pin (for the pickup map). */
    @Query("""
            select i from Item i join fetch i.category join fetch i.owner
            where i.approvalStatus = com.ecoswap.item.entity.ListingApprovalStatus.APPROVED
              and i.availabilityStatus = com.ecoswap.item.entity.ItemAvailabilityStatus.AVAILABLE
              and i.pickupLatitude is not null and i.pickupLongitude is not null
            order by i.createdAt desc, i.id desc
            """)
    List<Item> findMapPins(Pageable pageable);

    @Query("select i.approvalStatus, count(i) from Item i group by i.approvalStatus")
    List<Object[]> countByApprovalGrouped();

    @Query("select i.createdAt from Item i where i.createdAt >= :since")
    List<java.time.LocalDateTime> listingTimesSince(@Param("since") java.time.LocalDateTime since);
}
