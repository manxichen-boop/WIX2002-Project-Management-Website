package com.ecoswap.item.service;

import com.ecoswap.auth.security.AuthenticatedUser;
import com.ecoswap.common.exception.NotFoundException;
import com.ecoswap.common.response.PageResponse;
import com.ecoswap.common.util.Times;
import com.ecoswap.exchange.entity.ExchangeRequest;
import com.ecoswap.exchange.entity.ExchangeRequestStatus;
import com.ecoswap.exchange.repository.ExchangeRequestRepository;
import com.ecoswap.favorite.FavoriteRepository;
import com.ecoswap.item.dto.ImageView;
import com.ecoswap.item.dto.ItemCardView;
import com.ecoswap.item.dto.ItemDetailView;
import com.ecoswap.item.dto.ItemSearchCriteria;
import com.ecoswap.item.entity.Item;
import com.ecoswap.item.entity.ItemAvailabilityStatus;
import com.ecoswap.item.entity.ListingApprovalStatus;
import com.ecoswap.item.repository.ItemRepository;
import com.ecoswap.item.repository.ItemSpecifications;
import com.ecoswap.web.CurrentUserView;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only listing queries: Explore search, item details and the homepage. */
@Service
@Transactional(readOnly = true)
public class ItemQueryService {

    private static final List<ExchangeRequestStatus> ACTIVE = List.of(ExchangeRequestStatus.PENDING, ExchangeRequestStatus.ACCEPTED);

    private final ItemRepository items;
    private final ItemViewMapper mapper;
    private final FavoriteRepository favorites;
    private final ExchangeRequestRepository requests;

    public ItemQueryService(ItemRepository items, ItemViewMapper mapper, FavoriteRepository favorites, ExchangeRequestRepository requests) {
        this.items = items;
        this.mapper = mapper;
        this.favorites = favorites;
        this.requests = requests;
    }

    /** The newest approved, available listings (never sample data). */
    public List<ItemCardView> featured() {
        return mapper.cards(items.findTop8ByApprovalStatusAndAvailabilityStatusOrderByCreatedAtDesc(
                ListingApprovalStatus.APPROVED, ItemAvailabilityStatus.AVAILABLE));
    }

    /** Pickup map: approved, available listings with a pin (newest 300). */
    public List<ItemCardView> mapPins() {
        return mapper.cards(items.findMapPins(PageRequest.of(0, 300)));
    }

    /** Explore: only APPROVED + AVAILABLE items are ever returned. */
    public PageResponse<ItemCardView> search(ItemSearchCriteria c, Long viewerId) {
        Specification<Item> spec = Specification.allOf(
                ItemSpecifications.publiclyAvailable(),
                ItemSpecifications.textMatches(c.q()),
                ItemSpecifications.inCategory(c.category()),
                ItemSpecifications.hasCondition(c.condition()),
                ItemSpecifications.hasType(c.type()),
                ItemSpecifications.pickupMatches(c.location()));
        Sort sort = switch (c.sort() == null ? "newest" : c.sort()) {
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "name" -> Sort.by(Sort.Direction.ASC, "name");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
        PageRequest pageable = PageRequest.of(Math.max(c.page(), 0), Math.min(Math.max(c.size(), 1), 48), sort.and(Sort.by("id")));
        Page<Item> page = items.findAll(spec, pageable);
        List<ItemCardView> cards = mapper.cards(page.getContent());
        if (viewerId != null && !cards.isEmpty()) {
            Set<Long> saved = new HashSet<>(favorites.findSavedItemIds(viewerId, cards.stream().map(ItemCardView::id).toList()));
            cards = cards.stream().map(card -> card.withSaved(saved.contains(card.id()))).toList();
        }
        return new PageResponse<>(cards, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    /**
     * Item details. Approved listings are public; anything else (pending, rejected, suspended,
     * archived) is visible only to its owner and to administrators.
     */
    public ItemDetailView details(Long itemId, AuthenticatedUser viewer) {
        Item item = items.findWithOwnerAndCategory(itemId).orElseThrow(() -> new NotFoundException("Listing"));
        boolean isOwner = viewer != null && item.isOwnedBy(viewer.getId());
        boolean isAdmin = viewer != null && viewer.isAdmin();
        if (!item.isPubliclyVisible() && !isOwner && !isAdmin) {
            throw new NotFoundException("Listing");
        }

        boolean saved = false;
        ExchangeRequest active = null;
        if (viewer != null && !isAdmin) {
            saved = favorites.existsByUserIdAndItemId(viewer.getId(), itemId);
            active = requests.findFirstByItemIdAndRequesterIdAndStatusIn(itemId, viewer.getId(), ACTIVE).orElse(null);
        }
        String blocked = requestBlockedReason(item, viewer, isOwner, isAdmin, active);

        var owner = item.getOwner();
        CurrentUserView ownerView = CurrentUserView.of(owner);
        List<ImageView> images = item.getImages().stream().map(img -> new ImageView(img.getId(), ItemViewMapper.imageUrl(img.getId()))).toList();
        return new ItemDetailView(item.getId(), item.getName(), item.getDescription(),
                item.getCategory().getId(), item.getCategory().getName(), item.getCategory().getSlug(), item.getCategory().getIconKey(),
                item.getCondition().name(), item.getCondition().label(), item.getListingType(),
                item.getPickupLocation(), item.getPickupLatitude(), item.getPickupLongitude(), item.getApproximateWeight(),
                item.getApprovalStatus(), item.getAvailabilityStatus(),
                (isOwner || isAdmin) ? item.getRejectionReason() : null,
                Times.utc(item.getCreatedAt()), Times.utc(item.getUpdatedAt()), images,
                new ItemDetailView.Owner(owner.getId(), ownerView.firstName(), owner.getUniversity(), ownerView.initials(),
                        ownerView.avatarUrl(), Times.utc(owner.getCreatedAt())),
                new ItemDetailView.Viewer(viewer != null, isOwner, isAdmin, saved, blocked == null, blocked,
                        active == null ? null : active.getId(), active == null ? null : active.getStatus().name()));
    }

    private static String requestBlockedReason(Item item, AuthenticatedUser viewer, boolean isOwner, boolean isAdmin, ExchangeRequest active) {
        if (viewer == null) return "LOGIN_REQUIRED";
        if (isAdmin) return "ADMIN";
        if (isOwner) return "OWN_ITEM";
        if (active != null) return "ALREADY_REQUESTED";
        if (!item.isPubliclyAvailable()) return "NOT_AVAILABLE";
        return null;
    }
}
