package com.ecoswap.item;

import static org.assertj.core.api.Assertions.assertThat;

import com.ecoswap.category.Category;
import com.ecoswap.item.entity.Item;
import com.ecoswap.item.entity.ItemAvailabilityStatus;
import com.ecoswap.item.entity.ItemCondition;
import com.ecoswap.item.entity.ListingApprovalStatus;
import com.ecoswap.item.entity.ListingType;
import com.ecoswap.user.entity.User;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Domain rules for listing moderation, kept separate from availability. */
class ItemModerationRulesTest {

    private Category books;
    private Category furniture;
    private User admin;
    private Item item;

    @BeforeEach
    void setUp() {
        books = new Category("Books", "books", null, "books", 1);
        furniture = new Category("Furniture", "furniture", null, "furniture", 2);
        ReflectionTestUtils.setField(books, "id", 1L);
        ReflectionTestUtils.setField(furniture, "id", 2L);
        admin = User.newAdministrator("Admin", "admin@test", "hash");
        item = new Item(User.newStudent("Aisha", "a@test", "hash", "UM"), books, "Textbook", "Good condition textbook",
                ItemCondition.GOOD, ListingType.SWAP, "Library", null);
    }

    @Test
    void newListingsStartPendingAndAvailable() {
        assertThat(item.getApprovalStatus()).isEqualTo(ListingApprovalStatus.PENDING_APPROVAL);
        assertThat(item.getAvailabilityStatus()).isEqualTo(ItemAvailabilityStatus.AVAILABLE);
        assertThat(item.isPubliclyAvailable()).isFalse();
    }

    @Test
    void significantEditOfApprovedListingRequiresReapproval() {
        item.moderate(ListingApprovalStatus.APPROVED, admin, null);
        item.applyEdit(furniture, "Textbook", "Good condition textbook", ItemCondition.GOOD, ListingType.SWAP, "Library", null, false);
        assertThat(item.getApprovalStatus()).isEqualTo(ListingApprovalStatus.PENDING_APPROVAL);
    }

    @Test
    void minorEditKeepsApproval() {
        item.moderate(ListingApprovalStatus.APPROVED, admin, null);
        item.applyEdit(books, "Textbook", "Good condition textbook", ItemCondition.GOOD, ListingType.SWAP, "Engineering faculty",
                new BigDecimal("1.20"), false);
        assertThat(item.getApprovalStatus()).isEqualTo(ListingApprovalStatus.APPROVED);
        assertThat(item.isPubliclyAvailable()).isTrue();
    }

    @Test
    void anyEditResubmitsRejectedOrChangeRequestedListings() {
        item.moderate(ListingApprovalStatus.NEEDS_CHANGES, admin, "Add a photo");
        assertThat(item.getRejectionReason()).isEqualTo("Add a photo");
        item.applyEdit(books, "Textbook", "Good condition textbook", ItemCondition.GOOD, ListingType.SWAP, "Library", null, true);
        assertThat(item.getApprovalStatus()).isEqualTo(ListingApprovalStatus.PENDING_APPROVAL);
    }

    @Test
    void approvalClearsReasonAndReservationDoesNotTouchModeration() {
        item.moderate(ListingApprovalStatus.REJECTED, admin, "Blurry");
        item.moderate(ListingApprovalStatus.APPROVED, admin, null);
        assertThat(item.getRejectionReason()).isNull();
        item.reserve();
        assertThat(item.getApprovalStatus()).isEqualTo(ListingApprovalStatus.APPROVED);
        assertThat(item.isPubliclyAvailable()).isFalse();
        assertThat(item.isPubliclyVisible()).isTrue();
    }
}
