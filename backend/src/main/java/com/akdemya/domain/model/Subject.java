package com.akdemya.domain.model;

import java.util.UUID;

public class Subject {
  private final UUID id;
  private final String name;
  private final String description;

  public Subject(UUID id, String name, String description) {
    this.id = id;
    this.name = name;
    this.description = description;
  }

  public static Subject create(String name, String description) {
    return new Subject(UUID.randomUUID(), name, description);
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
}
