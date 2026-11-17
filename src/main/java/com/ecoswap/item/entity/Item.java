package com.ecoswap.item.entity;

import com.ecoswap.category.Category;
import com.ecoswap.user.entity.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "item_name", nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "item_condition", nullable = false, length = 20)
    private ItemCondition condition;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "listing_type", nullable = false, length = 20)
    private ListingType listingType;

    @Column(name = "pickup_location", nullable = false, length = 150)
    private String pickupLocation;

    @Column(name = "pickup_latitude", precision = 9, scale = 6)
    private BigDecimal pickupLatitude;

    @Column(name = "pickup_longitude", precision = 9, scale = 6)
    private BigDecimal pickupLongitude;

    @Column(name = "approximate_weight", precision = 8, scale = 2)
    private BigDecimal approximateWeight;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "approval_status", nullable = false, length = 20)
    private ListingApprovalStatus approvalStatus = ListingApprovalStatus.PENDING_APPROVAL;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "availability_status", nullable = false, length = 20)
    private ItemAvailabilityStatus availabilityStatus = ItemAvailabilityStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Version
    @Column(nullable = false)
    private long version;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    private List<ItemImage> images = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Item() {}

    public Item(User owner, Category category, String name, String description, ItemCondition condition,
                ListingType listingType, String pickupLocation, BigDecimal approximateWeight) {
        this.owner = owner;
        this.category = category;
        this.name = name;
        this.description = description;
        this.condition = condition;
        this.listingType = listingType;
        this.pickupLocation = pickupLocation;
        this.approximateWeight = approximateWeight;
    }

    // ---- state checks -------------------------------------------------------------------

    public boolean isOwnedBy(Long userId) { return owner.getId().equals(userId); }

    /** Visible in Explore and requestable. */
    public boolean isPubliclyAvailable() {
        return approvalStatus == ListingApprovalStatus.APPROVED && availabilityStatus == ItemAvailabilityStatus.AVAILABLE;
    }

    /** Visible to everyone on its detail page (also once reserved or completed, for history). */
    public boolean isPubliclyVisible() {
        return approvalStatus == ListingApprovalStatus.APPROVED && availabilityStatus != ItemAvailabilityStatus.ARCHIVED;
    }

    public boolean isEditableByOwner() {
        return availabilityStatus == ItemAvailabilityStatus.AVAILABLE && approvalStatus != ListingApprovalStatus.SUSPENDED;
    }

    // ---- owner actions ------------------------------------------------------------------

    /**
     * Applies an owner's edit. Significant changes to an approved listing send it back to
     * moderation, so unreviewed content is never published.
     */
    public void applyEdit(Category category, String name, String description, ItemCondition condition,
                          ListingType listingType, String pickupLocation, BigDecimal weight, boolean imagesChanged) {
        boolean significant = imagesChanged
                || !this.category.getId().equals(category.getId())
                || !this.name.equals(name)
                || !this.description.equals(description)
                || this.condition != condition
                || this.listingType != listingType;
        this.category = category;
        this.name = name;
        this.description = description;
        this.condition = condition;
        this.listingType = listingType;
        this.pickupLocation = pickupLocation;
        this.approximateWeight = weight;
        if (approvalStatus == ListingApprovalStatus.REJECTED || approvalStatus == ListingApprovalStatus.NEEDS_CHANGES
                || (approvalStatus == ListingApprovalStatus.APPROVED && significant)) {
            resubmit();
        }
    }

    public void resubmit() {
        this.approvalStatus = ListingApprovalStatus.PENDING_APPROVAL;
        this.reviewedBy = null;
        this.reviewedAt = null;
    }

    /** Sets or clears the optional map pin (rounded to 6 decimals, about 10 cm). */
    public void setPickupPin(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            this.pickupLatitude = null;
            this.pickupLongitude = null;
        } else {
            this.pickupLatitude = latitude.setScale(6, java.math.RoundingMode.HALF_UP);
            this.pickupLongitude = longitude.setScale(6, java.math.RoundingMode.HALF_UP);
        }
    }

    public boolean hasPickupPin() { return pickupLatitude != null && pickupLongitude != null; }

    public void archive() { this.availabilityStatus = ItemAvailabilityStatus.ARCHIVED; }

    // ---- moderation ---------------------------------------------------------------------

    public void moderate(ListingApprovalStatus decision, User admin, String reason) {
        this.approvalStatus = decision;
        this.reviewedBy = admin;
        this.reviewedAt = LocalDateTime.now();
        this.rejectionReason = decision == ListingApprovalStatus.APPROVED ? null : reason;
        if (decision == ListingApprovalStatus.APPROVED && availabilityStatus == ItemAvailabilityStatus.ARCHIVED) {
            this.availabilityStatus = ItemAvailabilityStatus.AVAILABLE;
        }
    }

    public void adminArchive(User admin, String reason) {
        this.availabilityStatus = ItemAvailabilityStatus.ARCHIVED;
        this.reviewedBy = admin;
        this.reviewedAt = LocalDateTime.now();
        this.rejectionReason = reason;
    }

    // ---- exchange lifecycle -------------------------------------------------------------

    public void reserve() { this.availabilityStatus = ItemAvailabilityStatus.RESERVED; }
    public void release() { this.availabilityStatus = ItemAvailabilityStatus.AVAILABLE; }
    public void markExchanged() { this.availabilityStatus = ItemAvailabilityStatus.COMPLETED; }

    public void addImage(ItemImage image) {
        image.attachTo(this);
        images.add(image);
    }

    public Long getId() { return id; }
    public User getOwner() { return owner; }
    public Category getCategory() { return category; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public ItemCondition getCondition() { return condition; }
    public ListingType getListingType() { return listingType; }
    public String getPickupLocation() { return pickupLocation; }
    public BigDecimal getPickupLatitude() { return pickupLatitude; }
    public BigDecimal getPickupLongitude() { return pickupLongitude; }
    public BigDecimal getApproximateWeight() { return approximateWeight; }
    public ListingApprovalStatus getApprovalStatus() { return approvalStatus; }
    public ItemAvailabilityStatus getAvailabilityStatus() { return availabilityStatus; }
    public User getReviewedBy() { return reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public long getVersion() { return version; }
    public List<ItemImage> getImages() { return images; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
