package com.ecoswap.item.dto;

import com.ecoswap.item.entity.ItemCondition;
import com.ecoswap.item.entity.ListingType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Listing fields submitted with a multipart form (photos are separate request parts). */
public record ItemForm(
        @NotBlank(message = "Give your item a name.") @Size(min = 3, max = 120, message = "Use 3–120 characters.") String name,
        @NotNull(message = "Choose a category.") Long categoryId,
        @NotBlank(message = "Describe the item.") @Size(min = 10, max = 2000, message = "Use 10–2000 characters.") String description,
        @NotNull(message = "Choose the condition.") ItemCondition condition,
        @NotNull(message = "Choose a listing type.") ListingType listingType,
        @NotBlank(message = "Where can it be picked up?") @Size(min = 2, max = 150, message = "Use 2–150 characters.") String pickupLocation,
        @DecimalMin(value = "0.01", message = "Weight must be more than 0 kg.")
        @DecimalMax(value = "500", message = "Weight must be 500 kg or less.")
        @Digits(integer = 3, fraction = 2, message = "Use up to 2 decimal places.") BigDecimal approximateWeight,
        @DecimalMin(value = "-90", message = "Choose a point on the map.") @DecimalMax(value = "90", message = "Choose a point on the map.") BigDecimal pickupLatitude,
        @DecimalMin(value = "-180", message = "Choose a point on the map.") @DecimalMax(value = "180", message = "Choose a point on the map.") BigDecimal pickupLongitude) {

    /** Both coordinates or neither. */
    public boolean hasHalfPin() {
        return (pickupLatitude == null) != (pickupLongitude == null);
    }
}
