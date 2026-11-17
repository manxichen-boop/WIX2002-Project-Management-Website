package com.ecoswap.favorite;

import com.ecoswap.common.exception.BusinessRuleException;
import com.ecoswap.common.exception.NotFoundException;
import com.ecoswap.common.response.PageResponse;
import com.ecoswap.item.dto.ItemCardView;
import com.ecoswap.item.entity.Item;
import com.ecoswap.item.repository.ItemRepository;
import com.ecoswap.item.service.ItemViewMapper;
import com.ecoswap.user.repository.UserRepository;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Saved items. Saving is idempotent; the unique (user, item) key prevents duplicates. */
@Service
public class FavoriteService {

    private final FavoriteRepository favorites;
    private final ItemRepository items;
    private final UserRepository users;
    private final ItemViewMapper mapper;

    public FavoriteService(FavoriteRepository favorites, ItemRepository items, UserRepository users, ItemViewMapper mapper) {
        this.favorites = favorites;
        this.items = items;
        this.users = users;
        this.mapper = mapper;
    }

    @Transactional
    public void save(Long userId, Long itemId) {
        Item item = items.findById(itemId).filter(Item::isPubliclyVisible).orElseThrow(() -> new NotFoundException("Listing"));
        if (item.isOwnedBy(userId)) {
            throw new BusinessRuleException("OWN_ITEM", "You can't save your own listing.");
        }
        if (!favorites.existsByUserIdAndItemId(userId, itemId)) {
            favorites.save(new Favorite(users.getReferenceById(userId), item));
        }
    }

    @Transactional
    public void remove(Long userId, Long itemId) {
        favorites.findByUserIdAndItemId(userId, itemId).ifPresent(favorites::delete);
    }

    /** Saved items with their current availability (saved items may since have been reserved or removed). */
    @Transactional(readOnly = true)
    public PageResponse<ItemCardView> list(Long userId, int page, int size) {
        var result = favorites.findByUserId(userId, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 48),
                Sort.by(Sort.Direction.DESC, "createdAt")));
        Map<Long, String> covers = mapper.coverUrls(result.getContent().stream().map(f -> f.getItem().getId()).toList());
        return PageResponse.of(result, f -> mapper.card(f.getItem(), covers.get(f.getItem().getId())).withSaved(true));
    }
}
