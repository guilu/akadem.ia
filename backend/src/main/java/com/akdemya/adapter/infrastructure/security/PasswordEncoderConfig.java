package com.akdemya.adapter.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Separate config to avoid the circular dependency:
 *   SecurityConfig → OAuth2SuccessHandler → AuthManager
 *     → SpringSecurityPasswordHasher → PasswordEncoder (@Bean in SecurityConfig).
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
