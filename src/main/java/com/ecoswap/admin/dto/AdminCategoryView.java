package com.ecoswap.admin.dto;

import com.ecoswap.category.Category;
import com.ecoswap.common.util.Times;
import java.time.Instant;

public record AdminCategoryView(Long id, String name, String slug, String description, String iconKey, int displayOrder,
                                boolean active, long itemCount, Instant createdAt) {

    public static AdminCategoryView of(Category c, long itemCount) {
        return new AdminCategoryView(c.getId(), c.getName(), c.getSlug(), c.getDescription(), c.getIconKey(), c.getDisplayOrder(),
                c.isActive(), itemCount, Times.utc(c.getCreatedAt()));
    }
}
