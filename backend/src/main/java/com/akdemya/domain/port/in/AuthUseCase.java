package com.akdemya.domain.port.in;

public interface AuthUseCase {
  AuthResponse register(RegisterCommand command);

  AuthResponse login(LoginCommand command);

  record RegisterCommand(String email, String password, String confirmPassword, String firstName, String lastName, String occupation) {
  }

  record LoginCommand(String email, String password) {
  }

  record AuthResponse(String accessToken, String role, String error) {
    public static AuthResponse success(String token, String role) {
      return new AuthResponse(token, role, null);
    }

    public static AuthResponse fail(String error) {
      return new AuthResponse(null, null, error);
    }
  }
}
