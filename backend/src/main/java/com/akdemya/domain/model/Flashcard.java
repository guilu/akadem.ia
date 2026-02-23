package com.akdemya.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Flashcard {

  private final UUID id;
  private final UUID unitId;
  private final String front;
  private final String back;
  private final LocalDateTime createdAt;
  private final LocalDateTime updatedAt;

  public Flashcard(UUID id, UUID unitId, String front, String back,
                   LocalDateTime createdAt, LocalDateTime updatedAt) {
    if (id == null) throw new IllegalArgumentException("id cannot be null");
    if (unitId == null) throw new IllegalArgumentException("unitId cannot be null");
    if (front == null || front.trim().isEmpty()) throw new IllegalArgumentException("front cannot be blank");
    if (back == null || back.trim().isEmpty()) throw new IllegalArgumentException("back cannot be blank");
    if (createdAt == null) throw new IllegalArgumentException("createdAt cannot be null");
    if (updatedAt == null) throw new IllegalArgumentException("updatedAt cannot be null");
    this.id = id;
    this.unitId = unitId;
    this.front = front.trim();
    this.back = back.trim();
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static Flashcard create(UUID unitId, String front, String back) {
    LocalDateTime now = LocalDateTime.now();
    return new Flashcard(UUID.randomUUID(), unitId, front, back, now, now);
  }

  public UUID getId() { return id; }
  public UUID getUnitId() { return unitId; }
  public String getFront() { return front; }
  public String getBack() { return back; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
}
