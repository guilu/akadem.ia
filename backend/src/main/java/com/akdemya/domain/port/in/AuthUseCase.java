package com.akdemya.domain.port.in;

public interface AuthUseCase {
  AuthResponse register(RegisterCommand command);

  AuthResponse login(LoginCommand command);

  AuthResponse loginWithOAuth2(String email, String name);

  record RegisterCommand(String email, String password, String confirmPassword, String firstName, String lastName, String occupation) {
  }

  record LoginCommand(String email, String password) {
  }

  record AuthResponse(String accessToken, String refreshToken, String role, String error) {
    public static AuthResponse success(String token, String refreshToken, String role) {
      return new AuthResponse(token, refreshToken, role, null);
    }

    public static AuthResponse fail(String error) {
      return new AuthResponse(null, null, null, error);
    }
  }
}
