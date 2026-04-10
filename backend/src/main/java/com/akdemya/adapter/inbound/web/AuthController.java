package com.akdemya.adapter.inbound.web;

import com.akdemya.adapter.infrastructure.security.OAuth2CodeStore;
import com.akdemya.domain.port.in.AuthUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthUseCase authUseCase;
    private final OAuth2CodeStore codeStore;

    public AuthController(AuthUseCase authUseCase, OAuth2CodeStore codeStore) {
        this.authUseCase = authUseCase;
        this.codeStore = codeStore;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthUseCase.RegisterCommand req) {
        var response = authUseCase.register(req);
        if (response.error() != null) {
            return ResponseEntity.badRequest().body(Map.of("error", response.error()));
        }
        return ResponseEntity.ok(Map.of("accessToken", response.accessToken(), "role", response.role()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthUseCase.LoginCommand req) {
        var response = authUseCase.login(req);
        if (response.error() != null) {
            return ResponseEntity.status(401).body(Map.of("error", response.error()));
        }
        return ResponseEntity.ok(Map.of("accessToken", response.accessToken(), "role", response.role()));
    }

    @PostMapping("/oauth2/exchange")
    public ResponseEntity<?> exchangeOAuth2Code(@RequestBody ExchangeCodeRequest req) {
        return codeStore.exchange(req.code())
            .map(result -> ResponseEntity.ok(
                (Object) Map.of("accessToken", result.accessToken(), "role", result.role())))
            .orElseGet(() -> ResponseEntity.badRequest().body(
                Map.of("error", "invalid_code")));
    }

    public record ExchangeCodeRequest(String code) {}
}
