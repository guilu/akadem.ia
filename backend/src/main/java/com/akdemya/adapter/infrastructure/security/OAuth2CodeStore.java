package com.akdemya.adapter.infrastructure.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OAuth2CodeStore {

    private final ConcurrentHashMap<String, CodeEntry> codes = new ConcurrentHashMap<>();

    public String store(String jwt, String refreshToken, String role) {
        String code = UUID.randomUUID().toString();
        codes.put(code, new CodeEntry(jwt, refreshToken, role, Instant.now().plusSeconds(60)));
        return code;
    }

    public Optional<ExchangeResult> exchange(String code) {
        CodeEntry entry = codes.remove(code);
        if (entry == null) {
            return Optional.empty();
        }
        if (Instant.now().isAfter(entry.expiresAt)) {
            return Optional.empty();
        }
        return Optional.of(new ExchangeResult(entry.jwt, entry.refreshToken, entry.role));
    }

    static class CodeEntry {
        final String jwt;
        final String refreshToken;
        final String role;
        Instant expiresAt;

        CodeEntry(String jwt, String refreshToken, String role, Instant expiresAt) {
            this.jwt = jwt;
            this.refreshToken = refreshToken;
            this.role = role;
            this.expiresAt = expiresAt;
        }
    }

    public record ExchangeResult(String accessToken, String refreshToken, String role) {}
}
