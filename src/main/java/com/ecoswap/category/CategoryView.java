package com.ecoswap.category;

/** Read model for category menus, filters and the homepage. */
public record CategoryView(Long id, String name, String slug, String description, String iconKey, int displayOrder, boolean active) {

    public static CategoryView of(Category c) {
        return new CategoryView(c.getId(), c.getName(), c.getSlug(), c.getDescription(), c.getIconKey(), c.getDisplayOrder(), c.isActive());
    }
}
