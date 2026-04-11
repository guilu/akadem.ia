package com.akdemya.adapter.infrastructure.security;

import com.akdemya.domain.port.out.TokenProvider;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService implements TokenProvider {
    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.ttl-minutes:15}")
    private int ttlMinutes;

    private long ttlMs() { return ttlMinutes * 60 * 1000L; }

    @PostConstruct
    void validateSecret() {
        byte[] decoded = Base64.getDecoder().decode(jwtSecret);
        if (decoded.length < 32) {
            throw new IllegalArgumentException(
                    "JWT secret must be at least 256 bits (32 bytes) when Base64-decoded");
        }
    }

    Key signingKey() {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret));
    }

    public String generate(String subject, Map<String, Object> claims) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + ttlMs()))
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parserBuilder().setSigningKey(signingKey()).build().parseClaimsJws(token);
    }
}
