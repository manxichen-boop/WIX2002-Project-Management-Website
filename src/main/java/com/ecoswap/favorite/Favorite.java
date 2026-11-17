package com.ecoswap.favorite;

import com.ecoswap.item.entity.Item;
import com.ecoswap.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "favorites", uniqueConstraints = @UniqueConstraint(name = "uk_favorites_user_item", columnNames = {"user_id", "item_id"}))
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favorite_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Favorite() {}

    public Favorite(User user, Item item) {
        this.user = user;
        this.item = item;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Item getItem() { return item; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
