package com.ecoswap.item.repository;

import com.ecoswap.common.util.Texts;
import com.ecoswap.item.entity.Item;
import com.ecoswap.item.entity.ItemAvailabilityStatus;
import com.ecoswap.item.entity.ItemCondition;
import com.ecoswap.item.entity.ListingApprovalStatus;
import com.ecoswap.item.entity.ListingType;
import org.springframework.data.jpa.domain.Specification;

/** Composable filters for Explore and the admin listing table. Null arguments mean "no filter". */
public final class ItemSpecifications {

    private ItemSpecifications() {}

    public static Specification<Item> publiclyAvailable() {
        return (root, q, cb) -> cb.and(
                cb.equal(root.get("approvalStatus"), ListingApprovalStatus.APPROVED),
                cb.equal(root.get("availabilityStatus"), ItemAvailabilityStatus.AVAILABLE));
    }

    public static Specification<Item> textMatches(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String pattern = Texts.likePattern(query);
        return (root, q, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern, '\\'),
                cb.like(cb.lower(root.get("description")), pattern, '\\'));
    }

    public static Specification<Item> inCategory(String slug) {
        if (slug == null || slug.isBlank()) {
            return null;
        }
        return (root, q, cb) -> cb.equal(root.get("category").get("slug"), slug);
    }

    public static Specification<Item> hasCondition(ItemCondition condition) {
        return condition == null ? null : (root, q, cb) -> cb.equal(root.get("condition"), condition);
    }

    public static Specification<Item> hasType(ListingType type) {
        return type == null ? null : (root, q, cb) -> cb.equal(root.get("listingType"), type);
    }

    public static Specification<Item> pickupMatches(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }
        String pattern = Texts.likePattern(location);
        return (root, q, cb) -> cb.like(cb.lower(root.get("pickupLocation")), pattern, '\\');
    }

    public static Specification<Item> hasApproval(ListingApprovalStatus status) {
        return status == null ? null : (root, q, cb) -> cb.equal(root.get("approvalStatus"), status);
    }

    public static Specification<Item> hasAvailability(ItemAvailabilityStatus status) {
        return status == null ? null : (root, q, cb) -> cb.equal(root.get("availabilityStatus"), status);
    }

    public static Specification<Item> ownerMatches(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String pattern = Texts.likePattern(query);
        return (root, q, cb) -> cb.or(
                cb.like(cb.lower(root.get("owner").get("fullName")), pattern, '\\'),
                cb.like(cb.lower(root.get("owner").get("email")), pattern, '\\'));
    }
}
