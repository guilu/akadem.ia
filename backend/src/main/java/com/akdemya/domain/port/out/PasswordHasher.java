package com.akdemya.domain.port.out;

public interface PasswordHasher {
  String encode(String rawPassword);

  boolean matches(String rawPassword, String encodedPassword);
}
