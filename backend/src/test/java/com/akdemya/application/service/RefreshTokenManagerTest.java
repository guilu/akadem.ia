package com.akdemya.application.service;

import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.model.RefreshToken;
import com.akdemya.domain.port.in.RefreshTokenUseCase;
import com.akdemya.domain.port.out.RefreshTokenRepository;
import com.akdemya.domain.port.out.TokenProvider;
import com.akdemya.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RefreshTokenManagerTest {

    private RefreshTokenRepository refreshTokenRepository;
    private TokenProvider tokenProvider;
    private UserRepository userRepository;
    private RefreshTokenManager manager;

    @BeforeEach
    void setUp() {
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        tokenProvider = mock(TokenProvider.class);
        userRepository = mock(UserRepository.class);
        manager = new RefreshTokenManager(refreshTokenRepository, tokenProvider, userRepository);
    }

    @Test
    void refresh_returnsNewAccessToken_whenValidToken() {
        String rawToken = "valid-raw-token";
        String hash = manager.hashToken(rawToken);
        UUID userId = UUID.randomUUID();

        RefreshToken refreshToken = new RefreshToken(UUID.randomUUID(), userId, hash,
            Instant.now().plusSeconds(3600), false);
        AppUser user = AppUser.create("user@test.com", "hash", "STUDENT", "Test", "User", null);

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(refreshToken));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenProvider.generate(anyString(), any())).thenReturn("new-access-token");

        RefreshTokenUseCase.RefreshResult result = manager.refresh(rawToken);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.role()).isEqualTo("STUDENT");
    }

    @Test
    void refresh_returnsError_whenTokenNotFound() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        RefreshTokenUseCase.RefreshResult result = manager.refresh("nonexistent-token");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.error()).isEqualTo("invalid_refresh_token");
    }

    @Test
    void refresh_returnsError_whenTokenExpired() {
        String rawToken = "expired-token";
        String hash = manager.hashToken(rawToken);
        UUID userId = UUID.randomUUID();

        RefreshToken expiredToken = new RefreshToken(UUID.randomUUID(), userId, hash,
            Instant.now().minusSeconds(1), false);

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(expiredToken));

        RefreshTokenUseCase.RefreshResult result = manager.refresh(rawToken);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.error()).isEqualTo("invalid_refresh_token");
    }

    @Test
    void refresh_returnsError_whenTokenRevoked() {
        String rawToken = "revoked-token";
        String hash = manager.hashToken(rawToken);
        UUID userId = UUID.randomUUID();

        RefreshToken revokedToken = new RefreshToken(UUID.randomUUID(), userId, hash,
            Instant.now().plusSeconds(3600), true);

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(revokedToken));

        RefreshTokenUseCase.RefreshResult result = manager.refresh(rawToken);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.error()).isEqualTo("invalid_refresh_token");
    }

    @Test
    void revoke_callsRevokeByTokenHash() {
        String rawToken = "token-to-revoke";
        String hash = manager.hashToken(rawToken);

        manager.revoke(rawToken);

        verify(refreshTokenRepository).revokeByTokenHash(hash);
    }
}
