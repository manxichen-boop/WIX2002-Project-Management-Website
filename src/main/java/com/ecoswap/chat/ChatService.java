package com.ecoswap.chat;

import com.ecoswap.common.exception.BusinessRuleException;
import com.ecoswap.common.exception.InvalidInputException;
import com.ecoswap.common.exception.NotFoundException;
import com.ecoswap.common.util.Times;
import com.ecoswap.exchange.entity.ExchangeRequest;
import com.ecoswap.exchange.entity.ExchangeRequestStatus;
import com.ecoswap.exchange.repository.ExchangeRequestRepository;
import com.ecoswap.item.entity.Item;
import com.ecoswap.item.service.ItemViewMapper;
import com.ecoswap.notification.NotificationService;
import com.ecoswap.notification.NotificationType;
import com.ecoswap.user.entity.User;
import com.ecoswap.user.repository.UserRepository;
import com.ecoswap.web.CurrentUserView;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Private chat between the owner and the requester of an exchange. The thread opens when the
 * owner accepts the request and stays writable after completion (to sort out follow-ups).
 * Only the two participants can read it; administrators cannot.
 */
@Service
public class ChatService {

    static final int PAGE = 50;
    static final int MAX_LENGTH = 2000;
    static final int MAX_PER_MINUTE = 20;
    private static final Set<ExchangeRequestStatus> WRITABLE = Set.of(ExchangeRequestStatus.ACCEPTED, ExchangeRequestStatus.COMPLETED);

    private final ChatMessageRepository messages;
    private final ExchangeRequestRepository requests;
    private final UserRepository users;
    private final NotificationService notifications;
    private final ItemViewMapper mapper;

    public ChatService(ChatMessageRepository messages, ExchangeRequestRepository requests, UserRepository users,
                       NotificationService notifications, ItemViewMapper mapper) {
        this.messages = messages;
        this.requests = requests;
        this.users = users;
        this.notifications = notifications;
        this.mapper = mapper;
    }

    // ------------------------------------------------------------------ inbox

    @Transactional(readOnly = true)
    public List<ChatViews.Conversation> conversations(Long userId) {
        List<ExchangeRequest> list = requests.findConversations(userId, WRITABLE, PageRequest.of(0, 100));
        if (list.isEmpty()) {
            return List.of();
        }
        List<Long> ids = list.stream().map(ExchangeRequest::getId).toList();
        Map<Long, Long> unread = new HashMap<>();
        messages.countUnreadByRequest(ids, userId).forEach(r -> unread.put((Long) r[0], (Long) r[1]));
        Map<Long, ChatMessage> latest = messages.findLatestPerRequest(ids).stream()
                .collect(Collectors.toMap(m -> m.getRequest().getId(), Function.identity()));
        Map<Long, String> covers = covers(list);
        // Most recent activity first: last message time, or the request's last change when nobody has written yet.
        Map<Long, LocalDateTime> activity = new HashMap<>();
        for (ExchangeRequest r : list) {
            ChatMessage m = latest.get(r.getId());
            activity.put(r.getId(), m != null && m.getCreatedAt().isAfter(r.getUpdatedAt()) ? m.getCreatedAt() : r.getUpdatedAt());
        }
        return list.stream()
                .sorted(Comparator.comparing((ExchangeRequest r) -> activity.get(r.getId())).reversed())
                .map(r -> conversation(r, userId, latest.get(r.getId()), unread.getOrDefault(r.getId(), 0L), covers))
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return messages.countUnreadFor(userId);
    }

    // ------------------------------------------------------------------ thread

    /** Latest messages (or only those after {@code afterId}, for polling). Marks the other side's messages read. */
    @Transactional
    public ChatViews.Thread thread(Long userId, Long requestId, Long afterId) {
        ExchangeRequest request = participantRequest(userId, requestId);
        List<ChatMessage> page;
        boolean hasOlder;
        if (afterId != null) {
            page = messages.findByRequestIdAndIdGreaterThanOrderByIdAsc(requestId, afterId, PageRequest.of(0, 200));
            hasOlder = false;
        } else {
            page = new ArrayList<>(messages.findByRequestIdOrderByIdDesc(requestId, PageRequest.of(0, PAGE)));
            Collections.reverse(page);
            hasOlder = !page.isEmpty() && messages.existsByRequestIdAndIdLessThan(requestId, page.get(0).getId());
        }
        messages.markRead(requestId, userId, LocalDateTime.now());
        ChatMessage last = page.isEmpty() ? null : page.get(page.size() - 1);
        ChatViews.Conversation conv = conversation(request, userId, last, 0, covers(List.of(request)));
        return new ChatViews.Thread(conv, page.stream().map(m -> view(m, userId)).toList(), hasOlder);
    }

    @Transactional(readOnly = true)
    public List<ChatViews.Message> older(Long userId, Long requestId, Long beforeId) {
        participantRequest(userId, requestId);
        List<ChatMessage> page = new ArrayList<>(messages.findByRequestIdAndIdLessThanOrderByIdDesc(requestId, beforeId, PageRequest.of(0, PAGE)));
        Collections.reverse(page);
        return page.stream().map(m -> view(m, userId)).toList();
    }

    @Transactional
    public ChatViews.Message send(Long userId, Long requestId, String rawBody) {
        ExchangeRequest request = participantRequest(userId, requestId);
        if (!WRITABLE.contains(request.getStatus())) {
            throw new BusinessRuleException("CHAT_CLOSED", request.getStatus() == ExchangeRequestStatus.PENDING
                    ? "You can chat once the owner accepts the request."
                    : "This exchange is closed, so the chat is read-only.");
        }
        String body = rawBody == null ? "" : rawBody.strip();
        if (body.isEmpty()) {
            throw new InvalidInputException("body", "Write a message first.");
        }
        if (body.length() > MAX_LENGTH) {
            throw new InvalidInputException("body", "Messages can be up to " + MAX_LENGTH + " characters.");
        }
        if (messages.countBySenderIdAndCreatedAtAfter(userId, LocalDateTime.now().minusMinutes(1)) >= MAX_PER_MINUTE) {
            throw new BusinessRuleException("SLOW_DOWN", "You're sending messages very quickly. Please wait a moment.");
        }
        User sender = users.findById(userId).orElseThrow(() -> new NotFoundException("Account"));
        // Notify only when this starts a new unread run, so a burst of messages is one notification.
        boolean firstUnread = messages.countByRequestIdAndSenderIdAndReadAtIsNull(requestId, userId) == 0;
        ChatMessage saved = messages.save(new ChatMessage(request, sender, body));
        if (firstUnread) {
            User other = request.isOwner(userId) ? request.getRequester() : request.getItem().getOwner();
            notifications.notify(other, NotificationType.CHAT_MESSAGE, "New message from " + firstName(sender),
                    "About \"" + request.getItem().getName() + "\": " + preview(body), "/messages/" + requestId);
        }
        return view(saved, userId);
    }

    // ------------------------------------------------------------------ helpers

    private ExchangeRequest participantRequest(Long userId, Long requestId) {
        ExchangeRequest request = requests.findDetailed(requestId).orElseThrow(() -> new NotFoundException("Conversation"));
        if (!request.isParticipant(userId)) {
            throw new NotFoundException("Conversation");   // never reveal other students' chats
        }
        if (!WRITABLE.contains(request.getStatus())) {
            // Closed exchanges keep their history readable, but a chat that never started stays closed.
            if (!messages.existsByRequestIdAndIdLessThan(requestId, Long.MAX_VALUE)) {
                throw new BusinessRuleException("CHAT_NOT_OPEN", "The chat opens once the owner accepts the request.");
            }
        }
        return request;
    }

    private Map<Long, String> covers(List<ExchangeRequest> list) {
        List<Long> itemIds = new ArrayList<>();
        for (ExchangeRequest r : list) {
            itemIds.add(r.getItem().getId());
            if (r.getOfferedItem() != null) {
                itemIds.add(r.getOfferedItem().getId());
            }
        }
        return mapper.coverUrls(itemIds);
    }

    private ChatViews.Conversation conversation(ExchangeRequest r, Long userId, ChatMessage last, long unread, Map<Long, String> covers) {
        boolean iAmOwner = r.isOwner(userId);
        User other = iAmOwner ? r.getRequester() : r.getItem().getOwner();
        CurrentUserView o = CurrentUserView.of(other);
        ChatViews.LastMessage lastMessage = last == null ? null
                : new ChatViews.LastMessage(preview(last.getBody()), Times.utc(last.getCreatedAt()), last.getSender().getId().equals(userId));
        boolean confirmedByMe = iAmOwner ? r.isOwnerConfirmedCompletion() : r.isRequesterConfirmedCompletion();
        return new ChatViews.Conversation(r.getId(), r.getStatus().name(), WRITABLE.contains(r.getStatus()), iAmOwner,
                itemRef(r.getItem(), covers), r.getOfferedItem() == null ? null : itemRef(r.getOfferedItem(), covers),
                new ChatViews.Person(other.getId(), other.getFullName(), o.firstName(), o.initials(), o.avatarUrl()),
                r.isOwnerConfirmedCompletion(), r.isRequesterConfirmedCompletion(), confirmedByMe, lastMessage, unread);
    }

    private static ChatViews.ItemRef itemRef(Item item, Map<Long, String> covers) {
        return new ChatViews.ItemRef(item.getId(), item.getName(), covers.get(item.getId()), item.getListingType().name(), item.getPickupLocation());
    }

    private static ChatViews.Message view(ChatMessage m, Long userId) {
        return new ChatViews.Message(m.getId(), m.getSender().getId(), firstName(m.getSender()), m.getSender().getId().equals(userId),
                m.getBody(), Times.utc(m.getCreatedAt()), Times.utc(m.getReadAt()));
    }

    private static String firstName(User u) {
        return u.getFullName().strip().split("\\s+")[0];
    }

    private static String preview(String body) {
        String flat = body.replaceAll("\\s+", " ").strip();
        return flat.length() <= 90 ? flat : flat.substring(0, 89) + "…";
    }
}
