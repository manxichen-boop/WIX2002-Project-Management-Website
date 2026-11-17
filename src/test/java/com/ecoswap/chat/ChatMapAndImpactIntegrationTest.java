package com.ecoswap.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecoswap.common.exception.ApiException;
import com.ecoswap.exchange.dto.ExchangeRequestView;
import com.ecoswap.exchange.service.ExchangeService;
import com.ecoswap.impact.ImpactService;
import com.ecoswap.impact.ImpactSummary;
import com.ecoswap.item.entity.Item;
import com.ecoswap.item.entity.ListingType;
import com.ecoswap.notification.NotificationRepository;
import com.ecoswap.notification.NotificationService;
import com.ecoswap.support.IntegrationTestBase;
import com.ecoswap.user.entity.AccountStatus;
import com.ecoswap.user.entity.User;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** v1.1 features: exchange chat, pickup map pins and live impact figures. */
class ChatMapAndImpactIntegrationTest extends IntegrationTestBase {

    @Autowired ExchangeService exchanges;
    @Autowired ChatService chats;
    @Autowired ImpactService impact;
    @Autowired NotificationService notificationService;
    @Autowired NotificationRepository notifications;

    @Test
    void chatOpensOnAcceptanceAndOnlyParticipantsCanUseIt() throws Exception {
        User admin = admin();
        User owner = student("Aisha Karim", AccountStatus.ACTIVE);
        User requester = student("Ben Tan", AccountStatus.ACTIVE);
        User stranger = student("Chen Wei", AccountStatus.ACTIVE);
        Item item = approvedItem(owner, ListingType.GIVEAWAY, admin);
        ExchangeRequestView r = exchanges.create(requester.getId(), item.getId(), null, "May I have it?");

        // closed while pending
        assertCode(() -> chats.send(requester.getId(), r.id(), "hello"), "CHAT_NOT_OPEN");
        assertThat(chats.conversations(requester.getId())).isEmpty();

        exchanges.accept(owner.getId(), r.id());
        long notesBefore = notifications.countByUserIdAndReadFalse(owner.getId());
        chats.send(requester.getId(), r.id(), "Hi! When can we meet?");
        chats.send(requester.getId(), r.id(), "I'm free after 3pm.");
        // a burst of messages is a single notification
        assertThat(notifications.countByUserIdAndReadFalse(owner.getId())).isEqualTo(notesBefore + 1);
        assertThat(chats.unreadCount(owner.getId())).isEqualTo(2);
        assertThat(notificationService.counts(owner.getId()).unreadMessages()).isEqualTo(2);
        assertThat(chats.conversations(owner.getId())).singleElement()
                .satisfies(c -> { assertThat(c.unread()).isEqualTo(2); assertThat(c.canSend()).isTrue(); assertThat(c.other().fullName()).isEqualTo("Ben Tan"); });

        // opening the thread marks it read; polling with "after" only returns newer messages
        ChatViews.Thread thread = chats.thread(owner.getId(), r.id(), null);
        assertThat(thread.messages()).extracting(ChatViews.Message::body).containsExactly("Hi! When can we meet?", "I'm free after 3pm.");
        assertThat(chats.unreadCount(owner.getId())).isZero();
        Long lastId = thread.messages().get(1).id();
        ChatViews.Message reply = chats.send(owner.getId(), r.id(), "3:30 at the library?");
        assertThat(chats.thread(requester.getId(), r.id(), lastId).messages()).extracting(ChatViews.Message::id).containsExactly(reply.id());
        assertThat(chats.thread(owner.getId(), r.id(), null).messages().get(0).readAt()).isNotNull();   // "Seen"

        // outsiders and administrators cannot read it
        assertCode(() -> chats.thread(stranger.getId(), r.id(), null), "NOT_FOUND");
        mvc.perform(get("/api/chats/" + r.id()).with(as(stranger))).andExpect(status().isNotFound());
        mvc.perform(get("/api/chats/" + r.id()).with(as(admin))).andExpect(status().isForbidden());
        mvc.perform(get("/api/chats/" + r.id())).andExpect(status().isUnauthorized());

        // validation over HTTP
        mvc.perform(post("/api/chats/" + r.id() + "/messages").with(as(owner)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("body", "   "))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/chats/" + r.id() + "/messages").with(as(owner)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("body", "x".repeat(2001)))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/chats/" + r.id() + "/messages").with(as(owner)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("body", "<b>See you</b>"))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.body").value("<b>See you</b>")).andExpect(jsonPath("$.mine").value(true));

        // withdrawn exchange: history stays readable, sending is blocked
        exchanges.cancel(owner.getId(), r.id(), "Plans changed, sorry");
        assertThat(chats.thread(requester.getId(), r.id(), null).conversation().canSend()).isFalse();
        assertCode(() -> chats.send(requester.getId(), r.id(), "ok"), "CHAT_CLOSED");
    }

    @Test
    void completedExchangeUpdatesImpactFiguresImmediately() {
        User admin = admin();
        User owner = student("Aisha Karim", AccountStatus.ACTIVE);
        User requester = student("Ben Tan", AccountStatus.ACTIVE);
        Item wanted = approvedItem(owner, ListingType.SWAP, admin);     // 1.50 kg
        Item offered = approvedItem(requester, ListingType.GIVEAWAY, admin);
        ImpactSummary before = impact.summary();

        ExchangeRequestView r = exchanges.create(requester.getId(), wanted.getId(), offered.getId(), "Swap?");
        exchanges.accept(owner.getId(), r.id());
        exchanges.confirmCompletion(owner.getId(), r.id());
        assertThat(impact.summary().completedExchanges()).isEqualTo(before.completedExchanges());   // one side is not enough
        exchanges.confirmCompletion(requester.getId(), r.id());

        ImpactSummary after = impact.summary();
        assertThat(after.completedExchanges()).isEqualTo(before.completedExchanges() + 1);
        assertThat(after.itemsReused()).isEqualTo(before.itemsReused() + 2);                          // a swap moves two items
        assertThat(after.reusedWeightKg()).isEqualByComparingTo(before.reusedWeightKg().add(new BigDecimal("3.0")));
        // chat stays open after completion for follow-ups
        assertThat(chats.send(requester.getId(), r.id(), "Thanks!").body()).isEqualTo("Thanks!");
    }

    @Test
    void pickupPinIsStoredValidatedAndPublishedOnTheMap() throws Exception {
        User admin = admin();
        User owner = student("Aisha Karim", AccountStatus.ACTIVE);
        Item pinned = approvedItem(owner, ListingType.GIVEAWAY, admin);
        pinned.setPickupPin(new BigDecimal("3.1209"), new BigDecimal("101.6538"));
        items.save(pinned);
        approvedItem(owner, ListingType.GIVEAWAY, admin);   // no pin: not on the map

        mvc.perform(get("/api/items/map"))   // public
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(pinned.getId()))
                .andExpect(jsonPath("$[0].pickupLatitude").value(3.1209))
                .andExpect(jsonPath("$[0].categoryIconKey").value("books"));
        mvc.perform(get("/pickup-map")).andExpect(status().isOk());

        MockMultipartFile photo = new MockMultipartFile("photos", "p.png", "image/png", tinyPng());
        Long booksId = categories.findBySlug("books").orElseThrow().getId();
        // only one coordinate -> rejected
        mvc.perform(MockMvcRequestBuilders.multipart("/api/items").file(photo).with(as(owner)).with(csrf())
                        .param("name", "Desk lamp").param("categoryId", booksId.toString()).param("description", "Warm white desk lamp.")
                        .param("condition", "GOOD").param("listingType", "GIVEAWAY").param("pickupLocation", "Library")
                        .param("pickupLatitude", "3.12"))
                .andExpect(status().isBadRequest());
        // out of range -> rejected
        mvc.perform(MockMvcRequestBuilders.multipart("/api/items").file(photo).with(as(owner)).with(csrf())
                        .param("name", "Desk lamp").param("categoryId", booksId.toString()).param("description", "Warm white desk lamp.")
                        .param("condition", "GOOD").param("listingType", "GIVEAWAY").param("pickupLocation", "Library")
                        .param("pickupLatitude", "123").param("pickupLongitude", "101.6"))
                .andExpect(status().isBadRequest());
        // valid pin -> stored with 6 decimals
        mvc.perform(MockMvcRequestBuilders.multipart("/api/items").file(photo).with(as(owner)).with(csrf())
                        .param("name", "Desk lamp").param("categoryId", booksId.toString()).param("description", "Warm white desk lamp.")
                        .param("condition", "GOOD").param("listingType", "GIVEAWAY").param("pickupLocation", "Library")
                        .param("pickupLatitude", "3.1225").param("pickupLongitude", "101.65601234"))
                .andExpect(status().isCreated());
        Item created = items.findAll().stream().filter(i -> i.getName().equals("Desk lamp")).findFirst().orElseThrow();
        assertThat(created.getPickupLatitude()).isEqualByComparingTo("3.122500");
        assertThat(created.getPickupLongitude()).isEqualByComparingTo("101.656012");
    }

    private static byte[] tinyPng() {
        return java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");
    }

    private static void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo(code));
    }
}
