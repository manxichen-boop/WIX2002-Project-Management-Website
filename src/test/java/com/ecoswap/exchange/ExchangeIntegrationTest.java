package com.ecoswap.exchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ecoswap.common.exception.ApiException;
import com.ecoswap.exchange.dto.ExchangeRequestView;
import com.ecoswap.exchange.entity.ExchangeRequestStatus;
import com.ecoswap.exchange.repository.ExchangeRequestRepository;
import com.ecoswap.exchange.service.ExchangeService;
import com.ecoswap.item.entity.Item;
import com.ecoswap.item.entity.ItemAvailabilityStatus;
import com.ecoswap.item.entity.ListingType;
import com.ecoswap.support.IntegrationTestBase;
import com.ecoswap.user.entity.AccountStatus;
import com.ecoswap.user.entity.User;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

/** Exchange rules end to end on MySQL, including real concurrent acceptance. */
class ExchangeIntegrationTest extends IntegrationTestBase {

    @Autowired ExchangeService exchanges;
    @Autowired ExchangeRequestRepository requests;

    @Test
    void fullSwapLifecycleCompletesOnlyAfterBothConfirm() {
        User admin = admin();
        User owner = student("Aisha Karim", AccountStatus.ACTIVE);
        User requester = student("Ben Tan", AccountStatus.ACTIVE);
        Item wanted = approvedItem(owner, ListingType.SWAP, admin);
        Item offered = approvedItem(requester, ListingType.GIVEAWAY, admin);

        ExchangeRequestView r = exchanges.create(requester.getId(), wanted.getId(), offered.getId(), "Swap?");
        exchanges.accept(owner.getId(), r.id());
        assertThat(items.findById(wanted.getId()).orElseThrow().getAvailabilityStatus()).isEqualTo(ItemAvailabilityStatus.RESERVED);
        assertThat(items.findById(offered.getId()).orElseThrow().getAvailabilityStatus()).isEqualTo(ItemAvailabilityStatus.RESERVED);

        assertThat(exchanges.confirmCompletion(owner.getId(), r.id()).status()).isEqualTo(ExchangeRequestStatus.ACCEPTED);
        assertThatThrownBy(() -> exchanges.confirmCompletion(owner.getId(), r.id())).hasMessageContaining("already confirmed");
        assertThat(exchanges.confirmCompletion(requester.getId(), r.id()).status()).isEqualTo(ExchangeRequestStatus.COMPLETED);

        assertThat(items.findById(wanted.getId()).orElseThrow().getAvailabilityStatus()).isEqualTo(ItemAvailabilityStatus.COMPLETED);
        assertThat(items.findById(offered.getId()).orElseThrow().getAvailabilityStatus()).isEqualTo(ItemAvailabilityStatus.COMPLETED);
        assertThat(exchanges.history(requester.getId(), 0, 10).totalElements()).isEqualTo(1);
    }

    @Test
    void businessRulesAreEnforcedOnTheServer() {
        User admin = admin();
        User owner = student("Aisha Karim", AccountStatus.ACTIVE);
        User requester = student("Ben Tan", AccountStatus.ACTIVE);
        Item swap = approvedItem(owner, ListingType.SWAP, admin);
        Item giveaway = approvedItem(owner, ListingType.GIVEAWAY, admin);
        Item othersItem = approvedItem(student("Chen Wei", AccountStatus.ACTIVE), ListingType.SWAP, admin);

        assertCode(() -> exchanges.create(owner.getId(), swap.getId(), null, null), "OWN_ITEM");
        assertCode(() -> exchanges.create(requester.getId(), swap.getId(), null, null), "VALIDATION_FAILED");              // swap needs offer
        assertCode(() -> exchanges.create(requester.getId(), swap.getId(), othersItem.getId(), null), "VALIDATION_FAILED"); // not theirs
        assertCode(() -> exchanges.create(requester.getId(), giveaway.getId(), othersItem.getId(), null), "VALIDATION_FAILED"); // giveaway + offer

        ExchangeRequestView r = exchanges.create(requester.getId(), giveaway.getId(), null, null);
        assertCode(() -> exchanges.create(requester.getId(), giveaway.getId(), null, null), "DUPLICATE_REQUEST");
        assertThatThrownBy(() -> exchanges.accept(requester.getId(), r.id())).hasMessageContaining("Only the item's owner");

        exchanges.cancel(requester.getId(), r.id(), null);
        assertThat(requests.findById(r.id()).orElseThrow().getStatus()).isEqualTo(ExchangeRequestStatus.CANCELLED);
        // a new request is allowed once the previous one is no longer active
        exchanges.create(requester.getId(), giveaway.getId(), null, "Asking again");
    }

    @Test
    void databaseRejectsDuplicateActiveRequestsEvenIfServiceIsBypassed() {
        User admin = admin();
        User owner = student("Aisha Karim", AccountStatus.ACTIVE);
        User requester = student("Ben Tan", AccountStatus.ACTIVE);
        Item item = approvedItem(owner, ListingType.GIVEAWAY, admin);
        requests.saveAndFlush(new com.ecoswap.exchange.entity.ExchangeRequest(item, requester, null, null));
        assertThatThrownBy(() -> requests.saveAndFlush(new com.ecoswap.exchange.entity.ExchangeRequest(item, requester, null, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void acceptingClosesConflictingRequestsAndWithdrawReleasesItems() {
        User admin = admin();
        User owner = student("Aisha Karim", AccountStatus.ACTIVE);
        User ben = student("Ben Tan", AccountStatus.ACTIVE);
        User chen = student("Chen Wei", AccountStatus.ACTIVE);
        Item lamp = approvedItem(owner, ListingType.GIVEAWAY, admin);
        var fromBen = exchanges.create(ben.getId(), lamp.getId(), null, null);
        var fromChen = exchanges.create(chen.getId(), lamp.getId(), null, null);

        exchanges.accept(owner.getId(), fromBen.id());
        assertThat(requests.findById(fromChen.id()).orElseThrow().getStatus()).isEqualTo(ExchangeRequestStatus.CLOSED);

        assertCode(() -> exchanges.cancel(ben.getId(), fromBen.id(), null), "VALIDATION_FAILED");   // withdraw needs reason
        exchanges.cancel(ben.getId(), fromBen.id(), "Found one elsewhere");
        assertThat(items.findById(lamp.getId()).orElseThrow().getAvailabilityStatus()).isEqualTo(ItemAvailabilityStatus.AVAILABLE);
    }

    /** Two owners' decisions racing for the same item: exactly one reservation may win. */
    @Test
    void concurrentAcceptancesCannotReserveTheSameItemTwice() throws Exception {
        User admin = admin();
        User owner = student("Aisha Karim", AccountStatus.ACTIVE);
        Item lamp = approvedItem(owner, ListingType.GIVEAWAY, admin);
        List<Long> requestIds = new ArrayList<>();
        for (String name : List.of("Ben Tan", "Chen Wei", "Dina Lee", "Eva Ong")) {
            requestIds.add(exchanges.create(student(name, AccountStatus.ACTIVE).getId(), lamp.getId(), null, null).id());
        }
        assertThat(race(requestIds.stream().<Callable<Object>>map(id -> () -> exchanges.accept(owner.getId(), id)).toList())).isEqualTo(1);
        assertThat(requests.findAll().stream().filter(r -> r.getStatus() == ExchangeRequestStatus.ACCEPTED)).hasSize(1);
        assertThat(items.findById(lamp.getId()).orElseThrow().getAvailabilityStatus()).isEqualTo(ItemAvailabilityStatus.RESERVED);
    }

    /** One offered item in two swap requests to different owners who accept at the same time. */
    @Test
    void anOfferedItemCannotBeReservedByTwoSwapsAtOnce() throws Exception {
        User admin = admin();
        User trader = student("Ben Tan", AccountStatus.ACTIVE);
        User ownerA = student("Aisha Karim", AccountStatus.ACTIVE);
        User ownerC = student("Chen Wei", AccountStatus.ACTIVE);
        Item offered = approvedItem(trader, ListingType.SWAP, admin);
        Item a = approvedItem(ownerA, ListingType.SWAP, admin);
        Item c = approvedItem(ownerC, ListingType.SWAP, admin);
        long toA = exchanges.create(trader.getId(), a.getId(), offered.getId(), null).id();
        long toC = exchanges.create(trader.getId(), c.getId(), offered.getId(), null).id();

        int successes = race(List.of(() -> exchanges.accept(ownerA.getId(), toA), () -> exchanges.accept(ownerC.getId(), toC)));
        assertThat(successes).isEqualTo(1);
        long reservedTargets = List.of(a.getId(), c.getId()).stream()
                .filter(id -> items.findById(id).orElseThrow().getAvailabilityStatus() == ItemAvailabilityStatus.RESERVED).count();
        assertThat(reservedTargets).isEqualTo(1);
    }

    private int race(List<Callable<Object>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Object>> futures = new ArrayList<>();
        for (Callable<Object> task : tasks) {
            futures.add(pool.submit(() -> { start.await(); return task.call(); }));
        }
        start.countDown();
        int ok = 0;
        for (Future<Object> f : futures) {
            try {
                f.get();
                ok++;
            } catch (java.util.concurrent.ExecutionException e) {
                assertThat(e.getCause()).isInstanceOf(ApiException.class);   // a clean business error, not a crash
            }
        }
        pool.shutdown();
        return ok;
    }

    private static void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo(code));
    }
}
