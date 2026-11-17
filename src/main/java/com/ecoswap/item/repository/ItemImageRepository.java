package com.ecoswap.item.repository;

import com.ecoswap.item.entity.ItemImage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemImageRepository extends JpaRepository<ItemImage, Long> {

    @Query("select img from ItemImage img join fetch img.item i join fetch i.owner where img.id = :id")
    Optional<ItemImage> findWithItem(@Param("id") Long id);

    /** First photo of each listed item, for card thumbnails in one query. */
    @Query("""
            select img from ItemImage img
            where img.item.id in :itemIds
              and img.displayOrder = (select min(i2.displayOrder) from ItemImage i2 where i2.item.id = img.item.id)
            """)
    List<ItemImage> findCoverImages(@Param("itemIds") List<Long> itemIds);
}
