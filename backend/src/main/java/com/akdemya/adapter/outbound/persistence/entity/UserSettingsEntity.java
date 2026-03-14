package com.akdemya.adapter.outbound.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_settings")
public class UserSettingsEntity {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "new_cards_limit", nullable = false)
    private int newCardsLimit;

    @Column(name = "review_cards_limit", nullable = false)
    private int reviewCardsLimit;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UserSettingsEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public int getNewCardsLimit() { return newCardsLimit; }
    public void setNewCardsLimit(int newCardsLimit) { this.newCardsLimit = newCardsLimit; }

    public int getReviewCardsLimit() { return reviewCardsLimit; }
    public void setReviewCardsLimit(int reviewCardsLimit) { this.reviewCardsLimit = reviewCardsLimit; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
