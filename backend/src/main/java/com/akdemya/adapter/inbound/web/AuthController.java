package com.akdemya.adapter.inbound.web;

import com.akdemya.domain.port.in.AuthUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    private final AuthUseCase authUseCase;

    public AuthController(AuthUseCase authUseCase) {
        this.authUseCase = authUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthUseCase.RegisterCommand req) {
        var response = authUseCase.register(req);
        if (response.error() != null) {
            return ResponseEntity.badRequest().body(Map.of("error", response.error()));
        }
        return ResponseEntity.ok(Map.of("accessToken", response.accessToken()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthUseCase.LoginCommand req) {
        var response = authUseCase.login(req);
        if (response.error() != null) {
            return ResponseEntity.status(401).body(Map.of("error", response.error()));
        }
        return ResponseEntity.ok(Map.of("accessToken", response.accessToken()));
    }
}
