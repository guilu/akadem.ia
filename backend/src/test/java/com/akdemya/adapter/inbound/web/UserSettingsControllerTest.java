package com.akdemya.adapter.inbound.web;

import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.port.in.UserSettingsUseCase;
import com.akdemya.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserSettingsControllerTest {

    private final UserSettingsUseCase settingsUseCase = mock(UserSettingsUseCase.class);
    private final UserRepository userRepo = mock(UserRepository.class);
    private final UserSettingsController controller = new UserSettingsController(settingsUseCase, userRepo);

    private final UUID userId = UUID.randomUUID();
    private final User principal = new User("user@test.com", "", List.of());

    private void setupUser() {
        when(userRepo.findByEmail("user@test.com"))
            .thenReturn(Optional.of(new AppUser(userId, "user@test.com", "", "STUDENT", null, null, null)));
    }

    @Test
    void getSettings_returnsPenaltyRatioInResponse() {
        setupUser();
        when(settingsUseCase.getSettings(userId))
            .thenReturn(new UserSettingsUseCase.GetSettingsResponse(20, 100, 3));

        ResponseEntity<UserSettingsController.SettingsResponse> response = controller.getSettings(principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().penaltyRatio());
    }

    @Test
    void getSettings_freshUser_returnsPenaltyRatio3() {
        setupUser();
        when(settingsUseCase.getSettings(userId))
            .thenReturn(new UserSettingsUseCase.GetSettingsResponse(20, 100, 3));

        ResponseEntity<UserSettingsController.SettingsResponse> response = controller.getSettings(principal);

        assertEquals(3, response.getBody().penaltyRatio());
    }

    @Test
    void updateSettings_withPenaltyRatioZero_returns400() {
        var req = new UserSettingsController.SettingsRequest(20, 100, 0);

        ResponseEntity<UserSettingsController.SettingsResponse> response = controller.updateSettings(req, principal);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(settingsUseCase, never()).updateSettings(any(), any());
    }

    @Test
    void updateSettings_withPenaltyRatio5_returns200WithPenaltyRatio5() {
        setupUser();
        when(settingsUseCase.updateSettings(eq(userId), any()))
            .thenReturn(new UserSettingsUseCase.GetSettingsResponse(20, 100, 5));

        var req = new UserSettingsController.SettingsRequest(20, 100, 5);
        ResponseEntity<UserSettingsController.SettingsResponse> response = controller.updateSettings(req, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5, response.getBody().penaltyRatio());
    }

    @Test
    void getSettings_unauthenticated_throws401() {
        assertThrows(ResponseStatusException.class, () -> controller.getSettings(null));
    }
}
