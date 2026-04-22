package com.akdemya.adapter.inbound.web;

import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.port.in.UserProfileUseCase;
import com.akdemya.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserProfileControllerTest {

  private final UserProfileUseCase userProfileUseCase = mock(UserProfileUseCase.class);
  private final UserRepository userRepo = mock(UserRepository.class);

  private final UserProfileController controller =
      new UserProfileController(userProfileUseCase, userRepo);

  @Test
  void getProfile_authenticated_returns200WithProfile() {
    UUID userId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(userRepo.findByEmail("user@example.com"))
        .thenReturn(Optional.of(new AppUser(userId, "user@example.com", "", "USER", "John", "Doe", null)));
    when(userProfileUseCase.getProfile(userId))
        .thenReturn(new UserProfileUseCase.GetProfileResponse(userId, "user@example.com", "John", "Doe"));

    ResponseEntity<UserProfileController.GetProfileResponse> response = controller.getProfile(principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("John", response.getBody().firstName());
    assertEquals("Doe", response.getBody().lastName());
    assertEquals("user@example.com", response.getBody().email());
  }

  @Test
  void getProfile_unauthenticated_throws401() {
    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> controller.getProfile(null));
    assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
  }

  @Test
  void updateProfile_validBody_returns200WithUpdatedProfile() {
    UUID userId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(userRepo.findByEmail("user@example.com"))
        .thenReturn(Optional.of(new AppUser(userId, "user@example.com", "", "USER", "Old", "Name", null)));
    when(userProfileUseCase.updateProfile(eq(userId), any()))
        .thenReturn(new UserProfileUseCase.GetProfileResponse(userId, "user@example.com", "Jane", "Smith"));

    var req = new UserProfileController.UpdateProfileRequest("Jane", "Smith");
    ResponseEntity<UserProfileController.GetProfileResponse> response = controller.updateProfile(req, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("Jane", response.getBody().firstName());
    assertEquals("Smith", response.getBody().lastName());
    verify(userProfileUseCase).updateProfile(eq(userId),
        eq(new UserProfileUseCase.UpdateProfileCommand("Jane", "Smith")));
  }

  @Test
  void updateProfile_unauthenticated_throws401() {
    var req = new UserProfileController.UpdateProfileRequest("Jane", "Smith");
    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> controller.updateProfile(req, null));
    assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
  }
}
