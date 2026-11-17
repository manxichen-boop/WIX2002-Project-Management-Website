package com.ecoswap.item.service;

import com.ecoswap.category.Category;
import com.ecoswap.category.CategoryService;
import com.ecoswap.common.exception.BusinessRuleException;
import com.ecoswap.common.exception.ForbiddenOperationException;
import com.ecoswap.common.exception.InvalidInputException;
import com.ecoswap.common.exception.NotFoundException;
import com.ecoswap.common.response.PageResponse;
import com.ecoswap.common.util.Texts;
import com.ecoswap.config.AppProperties;
import com.ecoswap.exchange.entity.ExchangeRequestStatus;
import com.ecoswap.exchange.repository.ExchangeRequestRepository;
import com.ecoswap.exchange.service.RequestCloser;
import com.ecoswap.item.dto.ItemCardView;
import com.ecoswap.item.dto.ItemForm;
import com.ecoswap.item.dto.MyListingView;
import com.ecoswap.item.entity.Item;
import com.ecoswap.item.entity.ItemAvailabilityStatus;
import com.ecoswap.item.entity.ItemImage;
import com.ecoswap.item.entity.ListingApprovalStatus;
import com.ecoswap.item.repository.ItemRepository;
import com.ecoswap.notification.NotificationService;
import com.ecoswap.notification.NotificationType;
import com.ecoswap.storage.FileCleanup;
import com.ecoswap.storage.StorageFolder;
import com.ecoswap.storage.StorageService;
import com.ecoswap.storage.StoredFile;
import com.ecoswap.storage.UploadValidator;
import com.ecoswap.user.entity.User;
import com.ecoswap.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Owner-side listing management: post, edit, resubmit, archive and "My Listings". */
@Service
public class ItemService {

    private final ItemRepository items;
    private final UserRepository users;
    private final CategoryService categories;
    private final UploadValidator uploads;
    private final StorageService storage;
    private final FileCleanup cleanup;
    private final NotificationService notifications;
    private final RequestCloser requestCloser;
    private final ExchangeRequestRepository requests;
    private final ItemViewMapper mapper;
    private final AppProperties props;

    public ItemService(ItemRepository items, UserRepository users, CategoryService categories, UploadValidator uploads,
                       StorageService storage, FileCleanup cleanup, NotificationService notifications,
                       RequestCloser requestCloser, ExchangeRequestRepository requests, ItemViewMapper mapper, AppProperties props) {
        this.items = items;
        this.users = users;
        this.categories = categories;
        this.uploads = uploads;
        this.storage = storage;
        this.cleanup = cleanup;
        this.notifications = notifications;
        this.requestCloser = requestCloser;
        this.requests = requests;
        this.mapper = mapper;
        this.props = props;
    }

    @Transactional
    public Long create(Long ownerId, ItemForm form, List<MultipartFile> photos) {
        User owner = users.findById(ownerId).orElseThrow(() -> new NotFoundException("Account"));
        List<MultipartFile> files = nonEmpty(photos);
        if (files.isEmpty()) {
            throw new InvalidInputException("photos", "Add at least one photo of the item.");
        }
        checkPhotoCount(files.size());
        Category category = categories.requireActive(form.categoryId());
        checkPin(form);

        Item item = new Item(owner, category, form.name().strip(), form.description().strip(), form.condition(),
                form.listingType(), form.pickupLocation().strip(), form.approximateWeight());
        item.setPickupPin(form.pickupLatitude(), form.pickupLongitude());
        storePhotos(item, files, 0);
        items.save(item);

        notifications.notifyAdmins(NotificationType.LISTING_SUBMITTED, "New listing to review",
                owner.getFullName() + " submitted \"" + item.getName() + "\".", "/admin/pending-listings");
        return item.getId();
    }

    /**
     * Applies an owner's edit. Photos can be removed (by id) and added in the same request.
     * Significant changes to an approved listing send it back to review (see Item.applyEdit).
     */
    @Transactional
    public void update(Long ownerId, Long itemId, ItemForm form, List<MultipartFile> newPhotos, List<Long> removeImageIds) {
        Item item = ownedItem(ownerId, itemId);
        if (!item.isEditableByOwner()) {
            throw new BusinessRuleException("ITEM_LOCKED", item.getApprovalStatus() == ListingApprovalStatus.SUSPENDED
                    ? "This listing was suspended by an administrator and can't be edited."
                    : "Listings that are reserved, exchanged or archived can't be edited.");
        }
        checkPin(form);
        Set<Long> remove = removeImageIds == null ? Set.of() : new HashSet<>(removeImageIds);
        List<MultipartFile> additions = nonEmpty(newPhotos);
        int remaining = (int) item.getImages().stream().filter(img -> !remove.contains(img.getId())).count();
        if (remaining + additions.size() == 0) {
            throw new InvalidInputException("photos", "Keep at least one photo of the item.");
        }
        checkPhotoCount(remaining + additions.size());

        List<String> deletedKeys = new ArrayList<>();
        item.getImages().removeIf(img -> {
            if (remove.contains(img.getId())) {
                deletedKeys.add(img.getStorageKey());
                return true;
            }
            return false;
        });
        int order = 0;
        for (ItemImage img : item.getImages()) {
            img.moveTo(order++);
        }
        storePhotos(item, additions, order);
        cleanup.deleteAfterCommit(deletedKeys);

        ListingApprovalStatus before = item.getApprovalStatus();
        item.applyEdit(categories.requireActive(form.categoryId()), form.name().strip(), form.description().strip(),
                form.condition(), form.listingType(), form.pickupLocation().strip(), form.approximateWeight(),
                !deletedKeys.isEmpty() || !additions.isEmpty());
        item.setPickupPin(form.pickupLatitude(), form.pickupLongitude());

        if (item.getApprovalStatus() == ListingApprovalStatus.PENDING_APPROVAL && before != ListingApprovalStatus.PENDING_APPROVAL) {
            // Back in review: nobody can request it until it is re-approved.
            requestCloser.closePendingFor(item, "the owner changed the listing and it is being reviewed again.");
            notifications.notifyAdmins(NotificationType.LISTING_SUBMITTED, "Listing resubmitted for review",
                    item.getOwner().getFullName() + " updated \"" + item.getName() + "\".", "/admin/pending-listings");
        }
    }

    private static void checkPin(ItemForm form) {
        if (form.hasHalfPin()) {
            throw new InvalidInputException("pickupLatitude", "Choose the pickup point on the map again.");
        }
    }

    /** Owner removes a listing (soft delete; exchange history stays intact). */
    @Transactional
    public void archive(Long ownerId, Long itemId) {
        Item item = ownedItem(ownerId, itemId);
        switch (item.getAvailabilityStatus()) {
            case RESERVED -> throw new BusinessRuleException("ITEM_RESERVED",
                    "This item is reserved for an accepted exchange. Withdraw from the exchange first.");
            case COMPLETED -> throw new BusinessRuleException("ITEM_EXCHANGED", "Exchanged items stay in your history.");
            case ARCHIVED -> { return; }
            case AVAILABLE -> { /* allowed */ }
        }
        requestCloser.closePendingFor(item, "the owner removed the listing.");
        item.archive();
    }

    @Transactional(readOnly = true)
    public PageResponse<MyListingView> myListings(Long ownerId, String tab, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Item> result = "archived".equals(tab)
                ? items.findByOwnerIdAndAvailabilityStatus(ownerId, ItemAvailabilityStatus.ARCHIVED, pageable)
                : items.findByOwnerIdAndAvailabilityStatusNot(ownerId, ItemAvailabilityStatus.ARCHIVED, pageable);
        List<Long> ids = result.getContent().stream().map(Item::getId).toList();
        Map<Long, String> covers = mapper.coverUrls(ids);
        Map<Long, Long> pending = ids.isEmpty() ? Map.of() : requests.countByItemIdsAndStatus(ids, ExchangeRequestStatus.PENDING)
                .stream().collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
        return PageResponse.of(result, i -> new MyListingView(mapper.card(i, covers.get(i.getId())),
                i.getApprovalStatus() == ListingApprovalStatus.APPROVED ? null : i.getRejectionReason(),
                pending.getOrDefault(i.getId(), 0L), i.isEditableByOwner(),
                i.getAvailabilityStatus() == ItemAvailabilityStatus.AVAILABLE));
    }

    /** The owner's approved, available items, offered when requesting a SWAP. */
    @Transactional(readOnly = true)
    public List<ItemCardView> availableForSwap(Long ownerId) {
        return mapper.cards(items.findByOwnerIdAndApprovalStatusAndAvailabilityStatusOrderByCreatedAtDesc(
                ownerId, ListingApprovalStatus.APPROVED, ItemAvailabilityStatus.AVAILABLE));
    }

    // ------------------------------------------------------------------ helpers

    private Item ownedItem(Long ownerId, Long itemId) {
        Item item = items.findById(itemId).orElseThrow(() -> new NotFoundException("Listing"));
        if (!item.isOwnedBy(ownerId)) {
            throw new ForbiddenOperationException("You can only change your own listings.");
        }
        return item;
    }

    private void storePhotos(Item item, List<MultipartFile> files, int startOrder) {
        List<String> stored = new ArrayList<>();
        cleanup.deleteOnRollback(stored);   // registered first; the list is filled below
        int order = startOrder;
        for (MultipartFile file : files) {
            UploadValidator.ValidatedUpload upload = uploads.image(file, "photos");
            StoredFile saved = storage.store(StorageFolder.ITEMS, upload.content(), upload.type(), upload.originalFilename());
            stored.add(saved.key());
            item.addImage(new ItemImage(saved.key(), Texts.clean(saved.originalFilename()), saved.mimeType(), saved.size(), order++));
        }
    }

    private void checkPhotoCount(int count) {
        int max = props.storage().maxImagesPerItem();
        if (count > max) {
            throw new InvalidInputException("photos", "You can add up to " + max + " photos.");
        }
    }

    private static List<MultipartFile> nonEmpty(List<MultipartFile> files) {
        return files == null ? List.of() : files.stream().filter(f -> f != null && !f.isEmpty()).toList();
    }
}
