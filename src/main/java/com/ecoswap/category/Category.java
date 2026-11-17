package com.ecoswap.category;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long id;

    @Column(name = "category_name", nullable = false, length = 80, unique = true)
    private String name;

    @Column(nullable = false, length = 80, unique = true)
    private String slug;

    @Column(length = 255)
    private String description;

    @Column(name = "icon_key", nullable = false, length = 40)
    private String iconKey;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Category() {}

    public Category(String name, String slug, String description, String iconKey, int displayOrder) {
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.iconKey = iconKey;
        this.displayOrder = displayOrder;
    }

    public void update(String name, String slug, String description, String iconKey, int displayOrder) {
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.iconKey = iconKey;
        this.displayOrder = displayOrder;
    }

    public void setActive(boolean active) { this.active = active; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getDescription() { return description; }
    public String getIconKey() { return iconKey; }
    public int getDisplayOrder() { return displayOrder; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
