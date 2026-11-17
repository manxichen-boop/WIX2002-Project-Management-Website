package com.ecoswap.exchange.service;

import com.ecoswap.common.exception.BusinessRuleException;
import com.ecoswap.common.exception.ForbiddenOperationException;
import com.ecoswap.common.exception.InvalidInputException;
import com.ecoswap.common.exception.NotFoundException;
import com.ecoswap.common.response.PageResponse;
import com.ecoswap.common.util.Texts;
import com.ecoswap.common.util.Times;
import com.ecoswap.exchange.dto.ExchangeRequestView;
import com.ecoswap.exchange.entity.ExchangeRequest;
import com.ecoswap.exchange.entity.ExchangeRequestStatus;
import com.ecoswap.exchange.repository.ExchangeRequestRepository;
import com.ecoswap.item.entity.Item;
import com.ecoswap.item.repository.ItemRepository;
import com.ecoswap.item.service.ItemViewMapper;
import com.ecoswap.notification.NotificationService;
import com.ecoswap.notification.NotificationType;
import com.ecoswap.user.entity.User;
import com.ecoswap.user.repository.UserRepository;
import com.ecoswap.web.CurrentUserView;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Student-to-student exchange requests. Every rule is enforced here (never only in the browser):
 * ownership, availability, duplicates, swap offers, transactional reservation with row locks,
 * and two-sided completion.
 */
@Service
public class ExchangeService {

    private static final List<ExchangeRequestStatus> ACTIVE = List.of(ExchangeRequestStatus.PENDING, ExchangeRequestStatus.ACCEPTED);

    private final ExchangeRequestRepository requests;
    private final ItemRepository items;
    private final UserRepository users;
    private final NotificationService notifications;
    private final ItemViewMapper mapper;
    private final EntityManager em;

    public ExchangeService(ExchangeRequestRepository requests, ItemRepository items, UserRepository users,
                           NotificationService notifications, ItemViewMapper mapper, EntityManager em) {
        this.requests = requests;
        this.items = items;
        this.users = users;
        this.notifications = notifications;
        this.mapper = mapper;
        this.em = em;
    }

    // ------------------------------------------------------------------ create

    @Transactional
    public ExchangeRequestView create(Long requesterId, Long itemId, Long offeredItemId, String rawMessage) {
        User requester = users.findById(requesterId).orElseThrow(() -> new NotFoundException("Account"));
        Item item = items.findById(itemId).orElseThrow(() -> new NotFoundException("Listing"));

        if (item.isOwnedBy(requesterId)) {
            throw new BusinessRuleException("OWN_ITEM", "You can't request your own listing.");
        }
        if (!item.isPubliclyAvailable()) {
            throw new BusinessRuleException("NOT_AVAILABLE", "This item is no longer available.");
        }
        if (requests.existsByItemIdAndRequesterIdAndStatusIn(itemId, requesterId, ACTIVE)) {
            throw new BusinessRuleException("DUPLICATE_REQUEST", "You already have an active request for this item.");
        }

        Item offered = null;
        if (item.getListingType().requiresOfferedItem()) {
            if (offeredItemId == null) {
                throw new InvalidInputException("offeredItemId", "Choose one of your items to offer for this swap.");
            }
            offered = items.findById(offeredItemId).orElseThrow(() -> new InvalidInputException("offeredItemId", "That item doesn't exist."));
            if (!offered.isOwnedBy(requesterId)) {
                throw new InvalidInputException("offeredItemId", "You can only offer your own items.");
            }
            if (!offered.isPubliclyAvailable()) {
                throw new InvalidInputException("offeredItemId", "The item you offer must be approved and available.");
            }
        } else if (offeredItemId != null) {
            throw new InvalidInputException("offeredItemId", "Giveaways and upcycling materials don't need an item in return.");
        }

        ExchangeRequest request = requests.save(new ExchangeRequest(item, requester, offered, Texts.clean(rawMessage)));
        notifications.notify(item.getOwner(), NotificationType.REQUEST_RECEIVED, "New request for \"" + item.getName() + "\"",
                requester.getFullName() + (offered != null ? " offers \"" + offered.getName() + "\" in exchange." : " would like this item."),
                "/my/received-requests");
        return view(request, requesterId, false);
    }

    // ------------------------------------------------------------------ owner decisions

    /**
     * Accepts a request. The request row and both item rows are locked (SELECT … FOR UPDATE, items
     * in id order to avoid deadlocks) and re-read, so two concurrent acceptances can never reserve
     * the same item. Conflicting pending requests are closed automatically.
     */
    @Transactional
    public ExchangeRequestView accept(Long ownerId, Long requestId) {
        LockedExchange locked = lockExchange(requestId);
        ExchangeRequest request = locked.request();
        if (!request.isOwner(ownerId)) {
            throw new ForbiddenOperationException("Only the item's owner can accept this request.");
        }
        requireStatus(request, ExchangeRequestStatus.PENDING, "This request is no longer pending.");

        List<Item> involved = locked.items();
        Item item = request.getItem();
        Item offered = request.getOfferedItem();
        if (!item.isPubliclyAvailable()) {
            throw new BusinessRuleException("NOT_AVAILABLE", "Your item is already reserved or no longer available.");
        }
        if (offered != null && (!offered.isPubliclyAvailable() || !offered.isOwnedBy(request.getRequester().getId()))) {
            throw new BusinessRuleException("OFFER_UNAVAILABLE", "The item offered in exchange is no longer available.");
        }
        if (!request.getRequester().isActive()) {
            throw new BusinessRuleException("REQUESTER_INACTIVE", "The requesting student's account is no longer active.");
        }

        request.accept();
        involved.forEach(Item::reserve);
        List<Long> ids = involved.stream().map(Item::getId).toList();
        for (ExchangeRequest other : requests.findPendingInvolving(ids, request.getId())) {
            other.finish(ExchangeRequestStatus.CLOSED, "the item was reserved through another exchange.");
            notifications.notify(other.getRequester(), NotificationType.REQUEST_CLOSED, "Request closed",
                    "\"" + other.getItem().getName() + "\" is no longer available, so your request was closed.", "/my/requests");
        }

        notifications.notify(request.getRequester(), NotificationType.REQUEST_ACCEPTED, "Request accepted!",
                item.getOwner().getFullName() + " accepted your request for \"" + item.getName()
                        + "\". Arrange the handover at " + item.getPickupLocation() + ".", "/my/requests");
        return view(request, ownerId, false);
    }

    @Transactional
    public ExchangeRequestView reject(Long ownerId, Long requestId, String reason) {
        ExchangeRequest request = lockRequest(requestId);
        if (!request.isOwner(ownerId)) {
            throw new ForbiddenOperationException("Only the item's owner can decline this request.");
        }
        requireStatus(request, ExchangeRequestStatus.PENDING, "This request is no longer pending.");
        String why = Texts.clean(reason);
        request.finish(ExchangeRequestStatus.REJECTED, why);
        notifications.notify(request.getRequester(), NotificationType.REQUEST_REJECTED, "Request declined",
                "Your request for \"" + request.getItem().getName() + "\" was declined." + (why == null ? "" : " Note: " + why), "/my/requests");
        return view(request, ownerId, false);
    }

    // ------------------------------------------------------------------ cancel / withdraw

    /**
     * PENDING: the requester cancels. ACCEPTED: either participant withdraws (reason required)
     * and both reserved items become available again.
     */
    @Transactional
    public ExchangeRequestView cancel(Long userId, Long requestId, String reason) {
        LockedExchange locked = lockExchange(requestId);
        ExchangeRequest request = locked.request();
        if (!request.isParticipant(userId)) {
            throw new NotFoundException("Request");
        }
        String why = Texts.clean(reason);
        if (request.getStatus() == ExchangeRequestStatus.PENDING) {
            if (!request.isRequester(userId)) {
                throw new ForbiddenOperationException("Owners decline pending requests instead of cancelling them.");
            }
            request.finish(ExchangeRequestStatus.CANCELLED, why);
            notifications.notify(request.getItem().getOwner(), NotificationType.REQUEST_CANCELLED, "Request cancelled",
                    request.getRequester().getFullName() + " cancelled their request for \"" + request.getItem().getName() + "\".",
                    "/my/received-requests");
        } else if (request.getStatus() == ExchangeRequestStatus.ACCEPTED) {
            if (why == null || why.length() < 3) {
                throw new InvalidInputException("reason", "Please tell the other student why you are withdrawing.");
            }
            locked.items().forEach(Item::release);
            request.finish(ExchangeRequestStatus.CANCELLED, why);
            User other = request.isOwner(userId) ? request.getRequester() : request.getItem().getOwner();
            User actor = request.isOwner(userId) ? request.getItem().getOwner() : request.getRequester();
            notifications.notify(other, NotificationType.REQUEST_CANCELLED, "Exchange withdrawn",
                    actor.getFullName() + " withdrew from the exchange for \"" + request.getItem().getName() + "\". Reason: " + why,
                    request.isOwner(userId) ? "/my/requests" : "/my/received-requests");
        } else {
            throw new BusinessRuleException("INVALID_STATE", "This request can no longer be cancelled.");
        }
        return view(request, userId, false);
    }

    // ------------------------------------------------------------------ completion

    /** Each participant confirms the handover; the exchange completes when both have. */
    @Transactional
    public ExchangeRequestView confirmCompletion(Long userId, Long requestId) {
        LockedExchange locked = lockExchange(requestId);
        ExchangeRequest request = locked.request();
        if (!request.isParticipant(userId)) {
            throw new NotFoundException("Request");
        }
        requireStatus(request, ExchangeRequestStatus.ACCEPTED, "Only accepted exchanges can be confirmed.");
        boolean alreadyConfirmed = request.isOwner(userId) ? request.isOwnerConfirmedCompletion() : request.isRequesterConfirmedCompletion();
        if (alreadyConfirmed) {
            throw new BusinessRuleException("ALREADY_CONFIRMED", "You have already confirmed this exchange.");
        }

        boolean completed = request.confirmCompletionBy(userId);
        Item item = request.getItem();
        if (completed) {
            locked.items().forEach(Item::markExchanged);
            String msg = "The exchange for \"" + item.getName() + "\" is complete. Thank you for reusing!";
            notifications.notify(request.getRequester(), NotificationType.EXCHANGE_COMPLETED, "Exchange completed", msg, "/my/history");
            notifications.notify(item.getOwner(), NotificationType.EXCHANGE_COMPLETED, "Exchange completed", msg, "/my/history");
        } else {
            User other = request.isOwner(userId) ? request.getRequester() : item.getOwner();
            User actor = request.isOwner(userId) ? item.getOwner() : request.getRequester();
            notifications.notify(other, NotificationType.EXCHANGE_CONFIRMATION, "Please confirm the handover",
                    actor.getFullName() + " confirmed the exchange for \"" + item.getName() + "\". Confirm it too once you have the item.",
                    request.isOwner(userId) ? "/my/requests" : "/my/received-requests");
        }
        return view(request, userId, false);
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public ExchangeRequestView get(Long userId, Long requestId) {
        ExchangeRequest request = requests.findDetailed(requestId).orElseThrow(() -> new NotFoundException("Request"));
        if (!request.isParticipant(userId)) {
            throw new NotFoundException("Request");   // don't reveal other students' exchanges
        }
        return view(request, userId, false);
    }

    @Transactional(readOnly = true)
    public PageResponse<ExchangeRequestView> sent(Long userId, ExchangeRequestStatus status, int page, int size) {
        return page(requests.findSent(userId, status, pageable(page, size)), userId);
    }

    @Transactional(readOnly = true)
    public PageResponse<ExchangeRequestView> received(Long userId, ExchangeRequestStatus status, int page, int size) {
        return page(requests.findReceived(userId, status, pageable(page, size)), userId);
    }

    @Transactional(readOnly = true)
    public PageResponse<ExchangeRequestView> history(Long userId, int page, int size) {
        return page(requests.findHistory(userId, List.of(ExchangeRequestStatus.ACCEPTED, ExchangeRequestStatus.COMPLETED),
                pageable(page, size)), userId);
    }

    /** Admin monitoring view (no actions, full participant details). */
    @Transactional(readOnly = true)
    public ExchangeRequestView adminView(ExchangeRequest request) {
        return view(request, null, true);
    }

    @Transactional(readOnly = true)
    public PageResponse<ExchangeRequestView> adminPage(Page<ExchangeRequest> page) {
        Map<Long, String> covers = covers(page.getContent());
        return PageResponse.of(page, r -> view(r, null, true, covers));
    }

    // ------------------------------------------------------------------ helpers

    private ExchangeRequest lockRequest(Long requestId) {
        return requests.findByIdForUpdate(requestId).orElseThrow(() -> new NotFoundException("Request"));
    }

    private record LockedExchange(ExchangeRequest request, List<Item> items) {}

    /**
     * Lock order for every state change that touches items: first the item rows (ascending id),
     * then the request row. A single global order means concurrent operations queue behind each
     * other instead of deadlocking; every check is then repeated on the freshly locked rows.
     * The ids are read without initialising the lazy item proxies, so the locking queries
     * load the items' current state.
     */
    private LockedExchange lockExchange(Long requestId) {
        ExchangeRequest request = requests.findById(requestId).orElseThrow(() -> new NotFoundException("Request"));
        List<Long> itemIds = new ArrayList<>();
        itemIds.add(request.getItem().getId());
        if (request.getOfferedItem() != null) {
            itemIds.add(request.getOfferedItem().getId());
        }
        itemIds.sort(Comparator.naturalOrder());
        List<Item> locked = itemIds.stream()
                .map(id -> items.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Listing")))
                .toList();
        em.refresh(request, LockModeType.PESSIMISTIC_WRITE);
        return new LockedExchange(request, locked);
    }

    private static void requireStatus(ExchangeRequest request, ExchangeRequestStatus expected, String message) {
        if (request.getStatus() != expected) {
            throw new BusinessRuleException("INVALID_STATE", message);
        }
    }

    private static PageRequest pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by(Sort.Direction.DESC, "updatedAt", "id"));
    }

    private PageResponse<ExchangeRequestView> page(Page<ExchangeRequest> page, Long userId) {
        Map<Long, String> covers = covers(page.getContent());
        return PageResponse.of(page, r -> view(r, userId, false, covers));
    }

    private Map<Long, String> covers(List<ExchangeRequest> list) {
        List<Long> ids = new ArrayList<>();
        list.forEach(r -> {
            ids.add(r.getItem().getId());
            if (r.getOfferedItem() != null) ids.add(r.getOfferedItem().getId());
        });
        return mapper.coverUrls(ids);
    }

    private ExchangeRequestView view(ExchangeRequest r, Long viewerId, boolean admin) {
        return view(r, viewerId, admin, covers(List.of(r)));
    }

    private ExchangeRequestView view(ExchangeRequest r, Long viewerId, boolean admin, Map<Long, String> covers) {
        boolean isOwner = viewerId != null && r.isOwner(viewerId);
        boolean isRequester = viewerId != null && r.isRequester(viewerId);
        ExchangeRequestStatus s = r.getStatus();
        boolean contactVisible = admin || s == ExchangeRequestStatus.ACCEPTED || s == ExchangeRequestStatus.COMPLETED;
        boolean myConfirm = isOwner ? r.isOwnerConfirmedCompletion() : r.isRequesterConfirmedCompletion();
        var actions = new ExchangeRequestView.Actions(
                isOwner && s == ExchangeRequestStatus.PENDING,
                isOwner && s == ExchangeRequestStatus.PENDING,
                isRequester && s == ExchangeRequestStatus.PENDING,
                (isOwner || isRequester) && s == ExchangeRequestStatus.ACCEPTED,
                (isOwner || isRequester) && s == ExchangeRequestStatus.ACCEPTED && !myConfirm);
        return new ExchangeRequestView(r.getId(), s, r.getStatusReason(), r.getMessage(),
                Times.utc(r.getCreatedAt()), Times.utc(r.getUpdatedAt()), Times.utc(r.getCompletedAt()),
                summary(r.getItem(), covers), r.getOfferedItem() == null ? null : summary(r.getOfferedItem(), covers),
                person(r.getItem().getOwner(), contactVisible), person(r.getRequester(), contactVisible),
                admin ? "ADMIN" : isOwner ? "OWNER" : "REQUESTER",
                r.isOwnerConfirmedCompletion(), r.isRequesterConfirmedCompletion(), actions);
    }

    private static ExchangeRequestView.ItemSummary summary(Item i, Map<Long, String> covers) {
        return new ExchangeRequestView.ItemSummary(i.getId(), i.getName(), covers.get(i.getId()), i.getListingType(),
                i.getCategory().getName(), i.getPickupLocation());
    }

    private static ExchangeRequestView.Person person(User u, boolean withContact) {
        CurrentUserView v = CurrentUserView.of(u);
        return new ExchangeRequestView.Person(u.getId(), u.getFullName(), u.getUniversity(), v.initials(), withContact ? u.getEmail() : null);
    }
}
