package com.akdemya.domain.port.in;

public interface AuthUseCase {
  AuthResponse register(RegisterCommand command);

  AuthResponse login(LoginCommand command);

  record RegisterCommand(String email, String password, String confirmPassword, String firstName, String lastName, String occupation) {
  }

  record LoginCommand(String email, String password) {
  }

  record AuthResponse(String accessToken, String error) {
    public static AuthResponse success(String token) {
      return new AuthResponse(token, null);
    }

    public static AuthResponse fail(String error) {
      return new AuthResponse(null, error);
    }
  }
}
