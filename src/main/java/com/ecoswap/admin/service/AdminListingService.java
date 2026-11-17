package com.ecoswap.admin.service;

import com.ecoswap.admin.dto.AdminItemView;
import com.ecoswap.approval.ApprovalDecision;
import com.ecoswap.approval.ApprovalEntityType;
import com.ecoswap.approval.ApprovalService;
import com.ecoswap.audit.AuditAction;
import com.ecoswap.audit.AuditService;
import com.ecoswap.audit.AuditTargetType;
import com.ecoswap.common.exception.BusinessRuleException;
import com.ecoswap.common.exception.NotFoundException;
import com.ecoswap.common.response.PageResponse;
import com.ecoswap.common.util.Times;
import com.ecoswap.exchange.service.RequestCloser;
import com.ecoswap.item.dto.ImageView;
import com.ecoswap.item.entity.Item;
import com.ecoswap.item.entity.ItemAvailabilityStatus;
import com.ecoswap.item.entity.ListingApprovalStatus;
import com.ecoswap.item.repository.ItemRepository;
import com.ecoswap.item.repository.ItemSpecifications;
import com.ecoswap.item.service.ItemViewMapper;
import com.ecoswap.notification.NotificationService;
import com.ecoswap.notification.NotificationType;
import com.ecoswap.user.entity.User;
import com.ecoswap.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Listing moderation: approve, reject, request changes, suspend and archive. */
@Service
@PreAuthorize("hasRole('ADMIN')")
public class AdminListingService {

    private final ItemRepository items;
    private final UserRepository users;
    private final ApprovalService approvals;
    private final AuditService audit;
    private final NotificationService notifications;
    private final RequestCloser requestCloser;

    public AdminListingService(ItemRepository items, UserRepository users, ApprovalService approvals, AuditService audit,
                               NotificationService notifications, RequestCloser requestCloser) {
        this.items = items;
        this.users = users;
        this.approvals = approvals;
        this.audit = audit;
        this.notifications = notifications;
        this.requestCloser = requestCloser;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminItemView> search(ListingApprovalStatus approval, ItemAvailabilityStatus availability, String q,
                                              int page, int size) {
        Specification<Item> spec = Specification.allOf(ItemSpecifications.hasApproval(approval),
                ItemSpecifications.hasAvailability(availability),
                Specification.anyOf(ItemSpecifications.textMatches(q), ItemSpecifications.ownerMatches(q)));
        // The review queue is first-come-first-served; other lists show the newest first.
        Sort sort = approval == ListingApprovalStatus.PENDING_APPROVAL ? Sort.by("updatedAt") : Sort.by(Sort.Direction.DESC, "createdAt");
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), sort.and(Sort.by("id")));
        return PageResponse.of(items.findAll(spec, pageable), i -> view(i, false));
    }

    @Transactional(readOnly = true)
    public AdminItemView detail(Long itemId) {
        return view(find(itemId), true);
    }

    @Transactional
    public AdminItemView approve(Long adminId, Long itemId) {
        User admin = admin(adminId);
        Item item = find(itemId);
        if (item.getApprovalStatus() == ListingApprovalStatus.APPROVED) {
            throw new BusinessRuleException("INVALID_STATE", "This listing is already approved.");
        }
        if (item.getAvailabilityStatus() == ItemAvailabilityStatus.ARCHIVED && item.getRejectionReason() == null) {
            throw new BusinessRuleException("INVALID_STATE", "The owner archived this listing.");
        }
        item.moderate(ListingApprovalStatus.APPROVED, admin, null);
        record(admin, item, ApprovalDecision.APPROVED, AuditAction.LISTING_APPROVED, null);
        notifications.notify(item.getOwner(), NotificationType.LISTING_APPROVED, "Listing approved",
                "\"" + item.getName() + "\" is now live in Explore.", "/items/" + item.getId());
        return view(item, true);
    }

    @Transactional
    public AdminItemView reject(Long adminId, Long itemId, String reason) {
        return decide(adminId, itemId, ListingApprovalStatus.REJECTED, ApprovalDecision.REJECTED, AuditAction.LISTING_REJECTED,
                NotificationType.LISTING_REJECTED, "Listing not approved", reason);
    }

    @Transactional
    public AdminItemView requestChanges(Long adminId, Long itemId, String reason) {
        return decide(adminId, itemId, ListingApprovalStatus.NEEDS_CHANGES, ApprovalDecision.NEEDS_CHANGES,
                AuditAction.LISTING_CHANGES_REQUESTED, NotificationType.LISTING_CHANGES_REQUESTED, "Changes requested", reason);
    }

    /** Hides an approved listing (e.g. after a report); open requests and reservations are released. */
    @Transactional
    public AdminItemView suspend(Long adminId, Long itemId, String reason) {
        Item item = find(itemId);
        if (item.getAvailabilityStatus() == ItemAvailabilityStatus.COMPLETED) {
            throw new BusinessRuleException("INVALID_STATE", "Completed exchanges can't be suspended.");
        }
        return decide(adminId, itemId, ListingApprovalStatus.SUSPENDED, ApprovalDecision.SUSPENDED, AuditAction.LISTING_SUSPENDED,
                NotificationType.LISTING_SUSPENDED, "Listing suspended", reason);
    }

    /** Removes prohibited content permanently from circulation. */
    @Transactional
    public AdminItemView archive(Long adminId, Long itemId, String reason) {
        User admin = admin(adminId);
        Item item = find(itemId);
        String why = AdminUserService.requireReason(reason);
        if (item.getAvailabilityStatus() == ItemAvailabilityStatus.COMPLETED) {
            throw new BusinessRuleException("INVALID_STATE", "Completed exchanges stay in the history.");
        }
        requestCloser.cancelAcceptedFor(item, "the listing was removed by an administrator.");
        requestCloser.closePendingFor(item, "the listing was removed by an administrator.");
        item.adminArchive(admin, why);
        record(admin, item, ApprovalDecision.ARCHIVED, AuditAction.LISTING_ARCHIVED, why);
        notifications.notify(item.getOwner(), NotificationType.LISTING_SUSPENDED, "Listing removed",
                "\"" + item.getName() + "\" was removed. Reason: " + why, "/my/listings");
        return view(item, true);
    }

    /**
     * Shared path for negative decisions. Any open requests are closed and accepted exchanges
     * cancelled, because a hidden listing must not stay reserved or requestable.
     */
    private AdminItemView decide(Long adminId, Long itemId, ListingApprovalStatus status, ApprovalDecision decision,
                                 AuditAction action, NotificationType type, String title, String reason) {
        User admin = admin(adminId);
        Item item = find(itemId);
        String why = AdminUserService.requireReason(reason);
        if (item.getApprovalStatus() == status) {
            throw new BusinessRuleException("INVALID_STATE", "The listing already has this status.");
        }
        requestCloser.cancelAcceptedFor(item, "the listing is no longer available (moderation).");
        requestCloser.closePendingFor(item, "the listing is no longer available (moderation).");
        item.moderate(status, admin, why);
        record(admin, item, decision, action, why);
        notifications.notify(item.getOwner(), type, title, "\"" + item.getName() + "\": " + why, "/my/listings");
        return view(item, true);
    }

    private void record(User admin, Item item, ApprovalDecision decision, AuditAction action, String reason) {
        approvals.record(ApprovalEntityType.ITEM, item.getId(), admin, decision, reason);
        audit.log(admin, action, AuditTargetType.ITEM, item.getId(), reason);
    }

    private AdminItemView view(Item i, boolean withHistory) {
        User owner = i.getOwner();
        return new AdminItemView(i.getId(), i.getName(), i.getDescription(), i.getCategory().getName(), i.getCondition().label(),
                i.getListingType(), i.getPickupLocation(), i.getApproximateWeight(), i.getApprovalStatus(), i.getAvailabilityStatus(),
                i.getRejectionReason(), Times.utc(i.getCreatedAt()), Times.utc(i.getUpdatedAt()),
                owner.getId(), owner.getFullName(), owner.getEmail(), owner.getUniversity(),
                i.getImages().stream().map(img -> new ImageView(img.getId(), ItemViewMapper.imageUrl(img.getId()))).toList(),
                withHistory ? approvals.historyOf(ApprovalEntityType.ITEM, i.getId()) : java.util.List.of());
    }

    private Item find(Long itemId) {
        return items.findById(itemId).orElseThrow(() -> new NotFoundException("Listing"));
    }

    private User admin(Long adminId) {
        return users.findById(adminId).orElseThrow(() -> new NotFoundException("Administrator"));
    }
}
