package com.akdemya.adapter.inbound.web;

import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.port.in.UserSettingsUseCase;
import com.akdemya.domain.port.out.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/settings")
public class UserSettingsController {

    private final UserSettingsUseCase settingsUseCase;
    private final UserRepository userRepo;

    public UserSettingsController(UserSettingsUseCase settingsUseCase, UserRepository userRepo) {
        this.settingsUseCase = settingsUseCase;
        this.userRepo = userRepo;
    }

    @GetMapping
    public ResponseEntity<SettingsResponse> getSettings(@AuthenticationPrincipal User principal) {
        UUID userId = requireUserId(principal);
        var result = settingsUseCase.getSettings(userId);
        return ResponseEntity.ok(new SettingsResponse(result.newCardsLimit(), result.reviewCardsLimit(), result.penaltyRatio()));
    }

    @PutMapping
    public ResponseEntity<SettingsResponse> updateSettings(@RequestBody SettingsRequest req,
                                                           @AuthenticationPrincipal User principal) {
        if (req == null) {
            return ResponseEntity.badRequest().build();
        }
        if (req.penaltyRatio() < 1) {
            return ResponseEntity.badRequest().build();
        }
        UUID userId = requireUserId(principal);
        var result = settingsUseCase.updateSettings(userId,
            new UserSettingsUseCase.UpdateSettingsCommand(req.newCardsLimit(), req.reviewCardsLimit(), req.penaltyRatio()));
        return ResponseEntity.ok(new SettingsResponse(result.newCardsLimit(), result.reviewCardsLimit(), result.penaltyRatio()));
    }

    private UUID requireUserId(User principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return userRepo.findByEmail(principal.getUsername())
            .map(AppUser::getId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    record SettingsResponse(int newCardsLimit, int reviewCardsLimit, int penaltyRatio) {}

    record SettingsRequest(int newCardsLimit, int reviewCardsLimit, int penaltyRatio) {}
}
