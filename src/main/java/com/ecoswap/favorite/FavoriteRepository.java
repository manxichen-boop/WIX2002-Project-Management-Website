package com.ecoswap.favorite;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByUserIdAndItemId(Long userId, Long itemId);

    boolean existsByUserIdAndItemId(Long userId, Long itemId);

    long countByUserId(Long userId);

    @EntityGraph(attributePaths = {"item", "item.category", "item.owner"})
    Page<Favorite> findByUserId(Long userId, Pageable pageable);

    @Query("select f.item.id from Favorite f where f.user.id = :userId and f.item.id in :itemIds")
    List<Long> findSavedItemIds(@Param("userId") Long userId, @Param("itemIds") Collection<Long> itemIds);
}
