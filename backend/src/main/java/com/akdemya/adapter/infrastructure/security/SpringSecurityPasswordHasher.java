package com.akdemya.adapter.infrastructure.security;

import com.akdemya.domain.port.out.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SpringSecurityPasswordHasher implements PasswordHasher {
  private final PasswordEncoder encoder;

  public SpringSecurityPasswordHasher(PasswordEncoder encoder) {
    this.encoder = encoder;
  }

  @Override
  public String encode(String rawPassword) {
    return encoder.encode(rawPassword);
  }

  @Override
  public boolean matches(String rawPassword, String encodedPassword) {
    return encoder.matches(rawPassword, encodedPassword);
  }
}
