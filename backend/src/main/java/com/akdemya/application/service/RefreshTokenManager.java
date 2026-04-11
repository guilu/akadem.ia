package com.akdemya.application.service;

import com.akdemya.domain.port.in.RefreshTokenUseCase;
import com.akdemya.domain.port.out.RefreshTokenRepository;
import com.akdemya.domain.port.out.TokenProvider;
import com.akdemya.domain.port.out.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

@Service
@Transactional
public class RefreshTokenManager implements RefreshTokenUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenProvider tokenProvider;
    private final UserRepository userRepository;

    public RefreshTokenManager(RefreshTokenRepository refreshTokenRepository,
                               TokenProvider tokenProvider,
                               UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
    }

    @Override
    public RefreshResult refresh(String rawToken) {
        String hash = hashToken(rawToken);
        var tokenOpt = refreshTokenRepository.findByTokenHash(hash);
        if (tokenOpt.isEmpty()) {
            return RefreshResult.failure("invalid_refresh_token");
        }
        var token = tokenOpt.get();
        if (!token.isValid()) {
            return RefreshResult.failure("invalid_refresh_token");
        }
        var userOpt = userRepository.findById(token.userId());
        if (userOpt.isEmpty()) {
            return RefreshResult.failure("user_not_found");
        }
        var user = userOpt.get();
        String newAccessToken = tokenProvider.generate(
            user.getEmail(),
            Map.of("uid", user.getId().toString(), "role", user.getRole())
        );
        return RefreshResult.success(newAccessToken, user.getRole());
    }

    @Override
    public void revoke(String rawToken) {
        String hash = hashToken(rawToken);
        refreshTokenRepository.revokeByTokenHash(hash);
    }

    String hashToken(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
