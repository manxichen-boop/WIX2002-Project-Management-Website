package com.ecoswap.exchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.ecoswap.category.Category;
import com.ecoswap.common.exception.BusinessRuleException;
import com.ecoswap.exchange.entity.ExchangeRequest;
import com.ecoswap.exchange.repository.ExchangeRequestRepository;
import com.ecoswap.exchange.service.ExchangeService;
import com.ecoswap.item.entity.Item;
import com.ecoswap.item.entity.ItemCondition;
import com.ecoswap.item.entity.ListingApprovalStatus;
import com.ecoswap.item.entity.ListingType;
import com.ecoswap.item.repository.ItemRepository;
import com.ecoswap.item.service.ItemViewMapper;
import com.ecoswap.notification.NotificationService;
import com.ecoswap.notification.NotificationType;
import com.ecoswap.user.entity.AccountStatus;
import com.ecoswap.user.entity.User;
import com.ecoswap.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

/** ExchangeService rules with mocked repositories (Mockito): creation, availability and duplicates. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExchangeServiceUnitTest {

    @Mock ExchangeRequestRepository requests;
    @Mock ItemRepository items;
    @Mock UserRepository users;
    @Mock NotificationService notifications;
    @Mock ItemViewMapper mapper;
    @Mock EntityManager em;

    ExchangeService service;
    User owner;
    User requester;
    Item giveaway;

    @BeforeEach
    void setUp() {
        service = new ExchangeService(requests, items, users, notifications, mapper, em);
        owner = withId(User.newStudent("Aisha Karim", "a@test", "h", "UM"), 1L);
        requester = withId(User.newStudent("Ben Tan", "b@test", "h", "UM"), 2L);
        owner.changeStatus(AccountStatus.ACTIVE, null);
        requester.changeStatus(AccountStatus.ACTIVE, null);
        Category cat = new Category("Books", "books", null, "books", 1);
        giveaway = new Item(owner, cat, "Lamp", "Working lamp", ItemCondition.GOOD, ListingType.GIVEAWAY, "Library", null);
        ReflectionTestUtils.setField(giveaway, "id", 10L);
        giveaway.moderate(ListingApprovalStatus.APPROVED, owner, null);
        when(users.findById(2L)).thenReturn(Optional.of(requester));
        when(items.findById(10L)).thenReturn(Optional.of(giveaway));
        when(mapper.coverUrls(anyList())).thenReturn(Map.of());
        when(requests.save(any(ExchangeRequest.class))).thenAnswer(inv -> withId(inv.getArgument(0), 99L));
    }

    @Test
    void createsRequestAndNotifiesOwner() {
        var view = service.create(2L, 10L, null, "  I need a lamp  ");
        assertThat(view.message()).isEqualTo("I need a lamp");
        verify(notifications).notify(eq(owner), eq(NotificationType.REQUEST_RECEIVED), any(), any(), eq("/my/received-requests"));
    }

    @Test
    void refusesUnavailableItemsWithoutSaving() {
        giveaway.reserve();
        assertThatThrownBy(() -> service.create(2L, 10L, null, null)).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no longer available");
        verify(requests, never()).save(any());
        verifyNoInteractions(notifications);
    }

    @Test
    void refusesDuplicateActiveRequests() {
        when(requests.existsByItemIdAndRequesterIdAndStatusIn(eq(10L), eq(2L), anyList())).thenReturn(true);
        assertThatThrownBy(() -> service.create(2L, 10L, null, null)).hasMessageContaining("already have an active request");
    }

    private static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
