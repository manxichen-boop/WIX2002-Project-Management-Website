package com.ecoswap.category;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByActiveTrueOrderByDisplayOrderAscNameAsc();

    List<Category> findAllByOrderByDisplayOrderAscNameAsc();

    Optional<Category> findBySlug(String slug);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    /** [categoryId, itemCount] for listings currently open in Explore. */
    @Query("""
            select i.category.id, count(i) from Item i
            where i.approvalStatus = com.ecoswap.item.entity.ListingApprovalStatus.APPROVED
              and i.availabilityStatus = com.ecoswap.item.entity.ItemAvailabilityStatus.AVAILABLE
            group by i.category.id
            """)
    List<Object[]> countAvailableItemsPerCategory();

    @Query("select count(i) from Item i where i.category.id = :categoryId")
    long countItemsUsing(Long categoryId);
}
