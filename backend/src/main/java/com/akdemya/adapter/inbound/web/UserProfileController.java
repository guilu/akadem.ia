package com.akdemya.adapter.inbound.web;

import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.port.in.UserProfileUseCase;
import com.akdemya.domain.port.out.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserProfileController {

  private final UserProfileUseCase userProfileUseCase;
  private final UserRepository userRepo;

  public UserProfileController(UserProfileUseCase userProfileUseCase, UserRepository userRepo) {
    this.userProfileUseCase = userProfileUseCase;
    this.userRepo = userRepo;
  }

  @GetMapping
  public ResponseEntity<GetProfileResponse> getProfile(@AuthenticationPrincipal User principal) {
    UUID userId = requireUserId(principal);
    var result = userProfileUseCase.getProfile(userId);
    return ResponseEntity.ok(toResponse(result));
  }

  @PatchMapping
  public ResponseEntity<GetProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest req,
      @AuthenticationPrincipal User principal) {
    UUID userId = requireUserId(principal);
    var result = userProfileUseCase.updateProfile(userId,
        new UserProfileUseCase.UpdateProfileCommand(req.firstName(), req.lastName()));
    return ResponseEntity.ok(toResponse(result));
  }

  private UUID requireUserId(User principal) {
    if (principal == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    return userRepo.findByEmail(principal.getUsername())
        .map(AppUser::getId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  private GetProfileResponse toResponse(UserProfileUseCase.GetProfileResponse result) {
    return new GetProfileResponse(result.id(), result.email(), result.firstName(), result.lastName());
  }

  record GetProfileResponse(UUID id, String email, String firstName, String lastName) {}

  record UpdateProfileRequest(String firstName, String lastName) {}
}
