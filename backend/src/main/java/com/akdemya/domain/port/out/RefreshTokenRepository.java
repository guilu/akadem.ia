package com.akdemya.domain.port.out;

import com.akdemya.domain.model.RefreshToken;
import java.util.Optional;

public interface RefreshTokenRepository {
    void save(RefreshToken token);
    Optional<RefreshToken> findByTokenHash(String hash);
    void revokeByTokenHash(String hash);
    void deleteExpired();
}
