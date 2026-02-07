package com.akdemya.adapter.outbound.persistence.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "users")
public class AppUserEntity {
    @Id
    private UUID id = UUID.randomUUID();
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false, name = "password_hash")
    private String passwordHash;
    @Column(nullable = false)
    private String role = "STUDENT";

    public AppUserEntity() {
    }

    public AppUserEntity(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public void setEmail(String e) {
        this.email = e;
    }

    public void setPasswordHash(String p) {
        this.passwordHash = p;
    }

    public void setRole(String r) {
        this.role = r;
    }
}
