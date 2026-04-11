package com.akdemya.application.service;

import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.model.RefreshToken;
import com.akdemya.domain.port.in.AuthUseCase;
import com.akdemya.domain.port.out.PasswordHasher;
import com.akdemya.domain.port.out.RefreshTokenRepository;
import com.akdemya.domain.port.out.TokenProvider;
import com.akdemya.domain.port.out.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class AuthManager implements AuthUseCase {

  private final UserRepository users;
  private final PasswordHasher hasher;
  private final TokenProvider tokenProvider;
  private final RefreshTokenRepository refreshTokenRepository;

  @Value("${security.refresh-token.ttl-days:7}")
  private int refreshTokenTtlDays;

  public AuthManager(UserRepository users, PasswordHasher hasher, TokenProvider tokenProvider,
                     RefreshTokenRepository refreshTokenRepository) {
    this.users = users;
    this.hasher = hasher;
    this.tokenProvider = tokenProvider;
    this.refreshTokenRepository = refreshTokenRepository;
  }

  private String hashToken(String raw) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashBytes);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  private String generateRawRefreshToken() {
    return UUID.randomUUID().toString() + UUID.randomUUID().toString();
  }

  @Override
  public AuthResponse register(RegisterCommand command) {
    if (command.email() == null || !command.email().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
      return AuthResponse.fail("invalid_email");
    }
    if (command.password() == null || command.password().length() < 8) {
      return AuthResponse.fail("password_too_short");
    }
    if (command.confirmPassword() == null || !command.password().equals(command.confirmPassword())) {
      return AuthResponse.fail("password_mismatch");
    }
    if (users.existsByEmail(command.email())) {
      return AuthResponse.fail("email_in_use");
    }
    AppUser user = AppUser.create(
        command.email(),
        hasher.encode(command.password()),
        "STUDENT",
        command.firstName(),
        command.lastName(),
        command.occupation()
    );
    users.save(user);
    String token = tokenProvider.generate(user.getEmail(),
        Map.of("uid", user.getId().toString(), "role", user.getRole()));
    String rawRefreshToken = generateRawRefreshToken();
    refreshTokenRepository.save(RefreshToken.create(user.getId(), hashToken(rawRefreshToken), refreshTokenTtlDays));
    return AuthResponse.success(token, rawRefreshToken, user.getRole());
  }

  @Override
  public AuthResponse login(LoginCommand command) {
    AppUser user = users.findByEmail(command.email()).orElse(null);
    if (user == null || !hasher.matches(command.password(), user.getPasswordHash())) {
      return AuthResponse.fail("invalid_credentials");
    }
    String token = tokenProvider.generate(user.getEmail(),
        Map.of("uid", user.getId().toString(), "role", user.getRole()));
    String rawRefreshToken = generateRawRefreshToken();
    refreshTokenRepository.save(RefreshToken.create(user.getId(), hashToken(rawRefreshToken), refreshTokenTtlDays));
    return AuthResponse.success(token, rawRefreshToken, user.getRole());
  }

  @Override
  public AuthResponse loginWithOAuth2(String email, String name) {
    AppUser user = users.findByEmail(email).orElse(null);
    if (user == null) {
      // First OAuth2 login: create user without password
      String firstName = name;
      String lastName = null;
      if (name != null && name.contains(" ")) {
        int idx = name.indexOf(' ');
        firstName = name.substring(0, idx);
        lastName = name.substring(idx + 1);
      }
      user = AppUser.create(email, null, "STUDENT", firstName, lastName, null);
      users.save(user);
    }
    String token = tokenProvider.generate(user.getEmail(),
        Map.of("uid", user.getId().toString(), "role", user.getRole()));
    String rawRefreshToken = generateRawRefreshToken();
    refreshTokenRepository.save(RefreshToken.create(user.getId(), hashToken(rawRefreshToken), refreshTokenTtlDays));
    return AuthResponse.success(token, rawRefreshToken, user.getRole());
  }
}
