package com.akdemya.domain.model;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {
    @Test void create_setsExpiresAtInFuture() {
        var token = RefreshToken.create(UUID.randomUUID(), "hash", 7);
        assertThat(token.expiresAt()).isAfter(Instant.now());
    }

    @Test void isExpired_returnsTrue_whenPastExpiry() {
        var token = new RefreshToken(null, UUID.randomUUID(), "hash", Instant.now().minusSeconds(1), false);
        assertThat(token.isExpired()).isTrue();
    }

    @Test void isValid_returnsFalse_whenExpired() {
        var token = new RefreshToken(null, UUID.randomUUID(), "hash", Instant.now().minusSeconds(1), false);
        assertThat(token.isValid()).isFalse();
    }

    @Test void isValid_returnsFalse_whenRevoked() {
        var token = new RefreshToken(null, UUID.randomUUID(), "hash", Instant.now().plusSeconds(3600), true);
        assertThat(token.isValid()).isFalse();
    }

    @Test void isValid_returnsTrue_whenActiveAndNotExpired() {
        var token = RefreshToken.create(UUID.randomUUID(), "hash", 7);
        assertThat(token.isValid()).isTrue();
    }
}
