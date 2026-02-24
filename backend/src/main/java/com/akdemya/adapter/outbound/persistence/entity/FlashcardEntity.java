package com.akdemya.adapter.outbound.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "flashcards")
public class FlashcardEntity {

  @Id
  private UUID id;

  @Column(name = "unit_id", nullable = false)
  private UUID unitId;

  @Column(columnDefinition = "text", nullable = false)
  private String front;

  @Column(columnDefinition = "text", nullable = false)
  private String back;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public FlashcardEntity() {}

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public UUID getUnitId() { return unitId; }
  public void setUnitId(UUID unitId) { this.unitId = unitId; }

  public String getFront() { return front; }
  public void setFront(String front) { this.front = front; }

  public String getBack() { return back; }
  public void setBack(String back) { this.back = back; }

  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
