package com.ecoswap.chat;

import java.time.Instant;
import java.util.List;

/** JSON shapes returned by the chat API. */
public final class ChatViews {

    private ChatViews() {}

    public record Message(Long id, Long senderId, String senderFirstName, boolean mine, String body,
                          Instant createdAt, Instant readAt) {}

    public record Person(Long id, String fullName, String firstName, String initials, String avatarUrl) {}

    public record ItemRef(Long id, String name, String coverUrl, String listingType, String pickupLocation) {}

    public record LastMessage(String preview, Instant createdAt, boolean mine) {}

    /**
     * One conversation (= one exchange request) as seen by the current student.
     * canSend is true while the exchange is accepted or completed.
     */
    public record Conversation(Long requestId, String status, boolean canSend, boolean iAmOwner,
                               ItemRef item, ItemRef offeredItem, Person other,
                               boolean ownerConfirmed, boolean requesterConfirmed, boolean confirmedByMe,
                               LastMessage lastMessage, long unread) {}

    public record Thread(Conversation conversation, List<Message> messages, boolean hasOlder) {}

    public record SendBody(String body) {}
}
