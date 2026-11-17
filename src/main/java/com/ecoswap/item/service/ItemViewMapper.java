package com.ecoswap.item.service;

import com.ecoswap.common.util.Times;
import com.ecoswap.item.dto.ItemCardView;
import com.ecoswap.item.entity.Item;
import com.ecoswap.item.entity.ItemImage;
import com.ecoswap.item.repository.ItemImageRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Builds card views for many items at once, fetching all cover photos in a single query. */
@Component
public class ItemViewMapper {

    private final ItemImageRepository images;

    public ItemViewMapper(ItemImageRepository images) {
        this.images = images;
    }

    public static String imageUrl(Long imageId) {
        return "/media/items/" + imageId;
    }

    public static String firstName(String fullName) {
        return fullName == null ? "" : fullName.strip().split("\\s+")[0];
    }

    public List<ItemCardView> cards(Collection<Item> items) {
        if (items.isEmpty()) {
            return List.of();
        }
        Map<Long, String> covers = coverUrls(items.stream().map(Item::getId).toList());
        return items.stream().map(i -> card(i, covers.get(i.getId()))).toList();
    }

    public Map<Long, String> coverUrls(List<Long> itemIds) {
        if (itemIds.isEmpty()) {
            return Map.of();
        }
        return images.findCoverImages(itemIds).stream()
                .collect(Collectors.toMap(img -> img.getItem().getId(), img -> imageUrl(img.getId()), (a, b) -> a));
    }

    public ItemCardView card(Item i, String coverUrl) {
        return new ItemCardView(i.getId(), i.getName(), i.getCategory().getName(), i.getCategory().getSlug(),
                i.getCondition().name(), i.getCondition().label(), i.getListingType(), i.getPickupLocation(),
                i.getPickupLatitude(), i.getPickupLongitude(), i.getCategory().getIconKey(), coverUrl,
                firstName(i.getOwner().getFullName()), i.getApprovalStatus(), i.getAvailabilityStatus(),
                Times.utc(i.getCreatedAt()), false);
    }

    public static Function<ItemImage, String> toUrl() {
        return img -> imageUrl(img.getId());
    }
}
