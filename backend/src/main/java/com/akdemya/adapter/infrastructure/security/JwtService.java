package com.akdemya.adapter.infrastructure.security;

import com.akdemya.domain.port.out.TokenProvider;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.security.Key;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService implements TokenProvider {
    @Value("${security.jwt.secret}")
    private String jwtSecret;

    private final long ttlMs = 1000L * 60 * 60 * 24;

    private Key signingKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generate(String subject, Map<String, Object> claims) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + ttlMs))
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parserBuilder().setSigningKey(signingKey()).build().parseClaimsJws(token);
    }
}
