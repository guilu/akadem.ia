package com.akdemya.domain.model;

import java.util.UUID;

public class AppUser {
  private final UUID id;
  private final String email;
  private final String passwordHash;
  private final String role;
  private final String firstName;
  private final String lastName;
  private final String occupation;

  public AppUser(UUID id, String email, String passwordHash, String role, String firstName, String lastName, String occupation) {
    this.id = id;
    this.email = email;
    this.passwordHash = passwordHash;
    this.role = role;
    this.firstName = firstName;
    this.lastName = lastName;
    this.occupation = occupation;
  }

  public static AppUser create(String email, String passwordHash, String role, String firstName, String lastName, String occupation) {
    return new AppUser(UUID.randomUUID(), email, passwordHash, role, firstName, lastName, occupation);
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

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getOccupation() {
    return occupation;
  }
}
