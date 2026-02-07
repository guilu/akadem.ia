package com.akdemya.domain.model;

import java.util.UUID;

public class AppUser {
  private final UUID id;
  private final String email;
  private final String passwordHash;
  private final String role;

  public AppUser(UUID id, String email, String passwordHash, String role) {
    this.id = id;
    this.email = email;
    this.passwordHash = passwordHash;
    this.role = role;
  }

  public static AppUser create(String email, String passwordHash, String role) {
    return new AppUser(UUID.randomUUID(), email, passwordHash, role);
  }

  public UUID getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getRole() {
    return role;
  }
}
