package com.akdemya.domain.model;

import java.time.Instant;
import java.util.UUID;

public record RefreshToken(UUID id, UUID userId, String tokenHash, Instant expiresAt, boolean revoked) {
    public static RefreshToken create(UUID userId, String tokenHash, int ttlDays) {
        return new RefreshToken(null, userId, tokenHash, Instant.now().plusSeconds((long) ttlDays * 86400), false);
    }

    public boolean isExpired() { return Instant.now().isAfter(expiresAt); }

    public boolean isValid() { return !revoked && !isExpired(); }
}
