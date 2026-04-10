package com.akdemya.adapter.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    // 32 bytes exactly — "akdemya-dev-secret-key-for-dev-!" (32 chars)
    private static final String VALID_SECRET =
            Base64.getEncoder().encodeToString("akdemya-dev-secret-key-for-dev-!".getBytes());

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
    }

    @Test
    void givenShortBase64Secret_whenValidate_thenThrowsIllegalArgumentException() {
        // "tooshort" is 8 bytes — well under 32
        String shortSecret = Base64.getEncoder().encodeToString("tooshort".getBytes());
        ReflectionTestUtils.setField(jwtService, "jwtSecret", shortSecret);

        assertThrows(IllegalArgumentException.class, jwtService::validateSecret);
    }

    @Test
    void givenValidBase64Secret_whenSigningKey_thenKeyIsAtLeast256Bits() {
        ReflectionTestUtils.setField(jwtService, "jwtSecret", VALID_SECRET);

        byte[] encoded = jwtService.signingKey().getEncoded();

        assertTrue(encoded.length >= 32, "Signing key must be at least 256 bits (32 bytes)");
    }

    @Test
    void givenValidSecret_whenGenerateAndParse_thenRoundTripSucceeds() {
        ReflectionTestUtils.setField(jwtService, "jwtSecret", VALID_SECRET);

        String token = jwtService.generate("user@test.com", Map.of("role", "STUDENT"));
        Jws<Claims> parsed = jwtService.parse(token);

        assertEquals("user@test.com", parsed.getBody().getSubject());
        assertEquals("STUDENT", parsed.getBody().get("role", String.class));
    }
}
