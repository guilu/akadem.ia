package com.akdemya.application.service;

import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.port.in.AuthUseCase;
import com.akdemya.domain.port.out.PasswordHasher;
import com.akdemya.domain.port.out.TokenProvider;
import com.akdemya.domain.port.out.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Transactional
public class AuthManager implements AuthUseCase {

  private final UserRepository users;
  private final PasswordHasher hasher;
  private final TokenProvider tokenProvider;

  public AuthManager(UserRepository users, PasswordHasher hasher, TokenProvider tokenProvider) {
    this.users = users;
    this.hasher = hasher;
    this.tokenProvider = tokenProvider;
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
    return AuthResponse.success(token);
  }

  @Override
  public AuthResponse login(LoginCommand command) {
    AppUser user = users.findByEmail(command.email()).orElse(null);
    if (user == null || !hasher.matches(command.password(), user.getPasswordHash())) {
      return AuthResponse.fail("invalid_credentials");
    }
    String token = tokenProvider.generate(user.getEmail(),
        Map.of("uid", user.getId().toString(), "role", user.getRole()));
    return AuthResponse.success(token);
  }
}
