package com.akdemya.domain.model;

import java.util.UUID;

public class Syllabus {
  private final UUID id;
  private final String name;
  private final String description;
  private final Visibility visibility;
  private final UUID ownerId;

  public Syllabus(UUID id, String name, String description, Visibility visibility, UUID ownerId) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.visibility = visibility;
    this.ownerId = ownerId;
  }

  public static Syllabus createGlobal(String name, String description) {
    return new Syllabus(UUID.randomUUID(), name, description, Visibility.GLOBAL, null);
  }

  public static Syllabus createPrivate(String name, String description, UUID ownerId) {
    if (ownerId == null) throw new IllegalArgumentException("ownerId cannot be null for PRIVATE syllabus");
    return new Syllabus(UUID.randomUUID(), name, description, Visibility.PRIVATE, ownerId);
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public Visibility getVisibility() {
    return visibility;
  }

  public UUID getOwnerId() {
    return ownerId;
  }
}
