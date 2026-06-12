package com.akdemya.adapter.inbound.web;

import com.akdemya.adapter.infrastructure.security.JwtService;
import com.akdemya.adapter.infrastructure.security.OAuth2CodeStore;
import com.akdemya.domain.port.in.AuthUseCase;
import com.akdemya.domain.port.in.RefreshTokenUseCase;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthUseCase authUseCase;
    private final OAuth2CodeStore codeStore;
    private final JwtService jwtService;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final boolean cookieSecure;

    public AuthController(AuthUseCase authUseCase,
                          OAuth2CodeStore codeStore,
                          JwtService jwtService,
                          RefreshTokenUseCase refreshTokenUseCase,
                          @Value("${app.cookie.secure:true}") boolean cookieSecure) {
        this.authUseCase = authUseCase;
        this.codeStore = codeStore;
        this.jwtService = jwtService;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody AuthUseCase.RegisterCommand req, HttpServletResponse res) {
        AuthUseCase.AuthResponse response;
        try {
            response = authUseCase.register(req);
        } catch (DataIntegrityViolationException e) {
            // Two concurrent registrations can both pass the existsByEmail
            // check; the loser hits uq_users_email at commit. The only unique
            // constraint reachable from this flow is the email one, so map it
            // to the same error the pre-check returns instead of a 500.
            return ResponseEntity.badRequest().body(Map.of("error", "email_in_use"));
        }
        if (response.error() != null) {
            return ResponseEntity.badRequest().body(Map.of("error", response.error()));
        }
        addAuthCookie(res, response.accessToken(), 900);
        addRefreshCookie(res, response.refreshToken(), 604800);
        return ResponseEntity.ok(Map.of("role", response.role()));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody AuthUseCase.LoginCommand req, HttpServletResponse res) {
        var response = authUseCase.login(req);
        if (response.error() != null) {
            return ResponseEntity.status(401).body(Map.of("error", response.error()));
        }
        addAuthCookie(res, response.accessToken(), 900);
        addRefreshCookie(res, response.refreshToken(), 604800);
        return ResponseEntity.ok(Map.of("role", response.role()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "ak_refresh", required = false) String rawRefreshToken,
                                     HttpServletResponse res) {
        if (rawRefreshToken != null) {
            refreshTokenUseCase.revoke(rawRefreshToken);
        }
        addAuthCookie(res, "", 0);
        addRefreshCookie(res, "", 0);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(@CookieValue(name = "ak_refresh", required = false) String rawToken,
                                      HttpServletResponse res) {
        if (rawToken == null) {
            return ResponseEntity.status(401).body(Map.of("error", "missing_refresh_token"));
        }
        var result = refreshTokenUseCase.refresh(rawToken);
        if (!result.isSuccess()) {
            return ResponseEntity.status(401).body(Map.of("error", result.error()));
        }
        addAuthCookie(res, result.accessToken(), 900);
        return ResponseEntity.ok(Map.of("role", result.role()));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> me(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) {
            return ResponseEntity.status(401).body(Map.of("error", "no_token"));
        }
        String token = Arrays.stream(cookies)
            .filter(c -> "ak_token".equals(c.getName()))
            .map(Cookie::getValue)
            .findFirst()
            .orElse(null);

        if (token == null || token.isBlank()) {
            return ResponseEntity.status(401).body(Map.of("error", "no_token"));
        }

        try {
            var jws = jwtService.parse(token);
            String email = jws.getBody().getSubject();
            String role  = String.valueOf(jws.getBody().get("role"));
            return ResponseEntity.ok(Map.of("email", email, "role", role));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_token"));
        }
    }

    @PostMapping("/oauth2/exchange")
    public ResponseEntity<Map<String, String>> exchangeOAuth2Code(@Valid @RequestBody ExchangeCodeRequest req, HttpServletResponse res) {
        return codeStore.exchange(req.code())
            .map(result -> {
                addAuthCookie(res, result.accessToken(), 900);
                addRefreshCookie(res, result.refreshToken(), 604800);
                return ResponseEntity.ok(Map.of("role", result.role()));
            })
            .orElseGet(() -> ResponseEntity.badRequest().body(
                Map.of("error", "invalid_code")));
    }

    private void addAuthCookie(HttpServletResponse res, String value, int maxAge) {
        Cookie cookie = new Cookie("ak_token", value);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/api");
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", "Strict");
        res.addCookie(cookie);
    }

    private void addRefreshCookie(HttpServletResponse res, String value, int maxAge) {
        ResponseCookie cookie = ResponseCookie.from("ak_refresh", value)
            .httpOnly(true)
            .secure(cookieSecure)
            .path("/api/auth")
            .maxAge(maxAge)
            .sameSite("Strict")
            .build();
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public record ExchangeCodeRequest(String code) {}
}
