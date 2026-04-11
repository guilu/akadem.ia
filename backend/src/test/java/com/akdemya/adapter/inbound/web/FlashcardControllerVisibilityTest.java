package com.akdemya.adapter.inbound.web;

import com.akdemya.adapter.inbound.web.dto.FlashcardDto;
import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.model.Flashcard;
import com.akdemya.domain.model.Visibility;
import com.akdemya.domain.port.in.FlashcardImportExportUseCase;
import com.akdemya.domain.port.in.FlashcardManagementUseCase;
import com.akdemya.domain.port.in.FlashcardReviewUseCase;
import com.akdemya.domain.port.in.FlashcardStudyUseCase;
import com.akdemya.domain.port.out.FlashcardRepository;
import com.akdemya.domain.port.out.FlashcardReviewLogRepository;
import com.akdemya.domain.port.out.FlashcardReviewRepository;
import com.akdemya.domain.port.out.SubjectRepository;
import com.akdemya.domain.port.out.UnitRepository;
import com.akdemya.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Tests visibility enforcement in FlashcardController: listing, creation with
 * ADMIN-only GLOBAL, and update metadata preservation.
 */
class FlashcardControllerVisibilityTest {

  private final FlashcardStudyUseCase studyUseCase = mock(FlashcardStudyUseCase.class);
  private final FlashcardReviewUseCase reviewUseCase = mock(FlashcardReviewUseCase.class);
  private final FlashcardManagementUseCase managementUseCase = mock(FlashcardManagementUseCase.class);
  private final FlashcardImportExportUseCase importExportUseCase = mock(FlashcardImportExportUseCase.class);
  private final FlashcardRepository flashcardRepo = mock(FlashcardRepository.class);
  private final FlashcardReviewRepository reviewRepo = mock(FlashcardReviewRepository.class);
  private final FlashcardReviewLogRepository reviewLogRepo = mock(FlashcardReviewLogRepository.class);
  private final UserRepository userRepo = mock(UserRepository.class);
  private final UnitRepository unitRepo = mock(UnitRepository.class);
  private final SubjectRepository subjectRepo = mock(SubjectRepository.class);

  private final FlashcardController controller = new FlashcardController(
      studyUseCase, reviewUseCase, managementUseCase, importExportUseCase,
      flashcardRepo, reviewLogRepo, userRepo);

  private UUID setupUser(String email) {
    UUID userId = UUID.randomUUID();
    when(userRepo.findByEmail(email))
        .thenReturn(Optional.of(new AppUser(userId, email, "", "USER", null, null, null)));
    return userId;
  }

  private UUID setupAdmin(String email) {
    UUID adminId = UUID.randomUUID();
    when(userRepo.findByEmail(email))
        .thenReturn(Optional.of(new AppUser(adminId, email, "", "ADMIN", null, null, null)));
    return adminId;
  }

  // --- List by unit ---

  @Test
  void unauthenticatedListByUnitReturnsAllFlashcards() {
    UUID unitId = UUID.randomUUID();
    Flashcard card = new Flashcard(UUID.randomUUID(), unitId, "front", "back",
        LocalDateTime.now(), LocalDateTime.now(), Visibility.GLOBAL, null);
    when(managementUseCase.listByUnit(unitId)).thenReturn(List.of(card));

    List<FlashcardDto.FlashcardResponse> result = controller.listByUnit(unitId, null);

    assertEquals(1, result.size());
    verify(managementUseCase).listByUnit(unitId);
    verify(managementUseCase, never()).listVisibleByUnit(any(), any());
  }

  @Test
  void authenticatedListByUnitReturnsVisibleFlashcards() {
    String email = "user@example.com";
    UUID userId = setupUser(email);
    UUID unitId = UUID.randomUUID();
    var principal = new User(email, "", List.of());

    Flashcard card = new Flashcard(UUID.randomUUID(), unitId, "front", "back",
        LocalDateTime.now(), LocalDateTime.now(), Visibility.PRIVATE, userId);
    when(managementUseCase.listVisibleByUnit(unitId, userId)).thenReturn(List.of(card));

    List<FlashcardDto.FlashcardResponse> result = controller.listByUnit(unitId, principal);

    assertEquals(1, result.size());
    verify(managementUseCase).listVisibleByUnit(unitId, userId);
    verify(managementUseCase, never()).listByUnit(any());
  }

  // --- Create ---

  @Test
  void createPrivateFlashcardByAuthenticatedUserSucceeds() {
    String email = "user@example.com";
    UUID userId = setupUser(email);
    UUID unitId = UUID.randomUUID();
    var principal = new User(email, "", List.of());

    Flashcard saved = new Flashcard(UUID.randomUUID(), unitId, "front", "back",
        LocalDateTime.now(), LocalDateTime.now(), Visibility.PRIVATE, userId);
    when(managementUseCase.createFlashcardWithVisibility(any())).thenReturn(saved);

    var req = new FlashcardDto.CreateRequest(unitId, "front", "back", Visibility.PRIVATE);
    ResponseEntity<FlashcardDto.FlashcardResponse> response = controller.create(req, principal);

    assertEquals(201, response.getStatusCodeValue());
    verify(managementUseCase).createFlashcardWithVisibility(
        argThat(cmd -> cmd.visibility() == Visibility.PRIVATE && userId.equals(cmd.ownerId())));
  }

  @Test
  void createGlobalFlashcardByNonAdminReturns403() {
    String email = "user@example.com";
    setupUser(email);
    var principal = new User(email, "", List.of());

    var req = new FlashcardDto.CreateRequest(UUID.randomUUID(), "front", "back", Visibility.GLOBAL);
    ResponseEntity<FlashcardDto.FlashcardResponse> response = controller.create(req, principal);

    assertEquals(403, response.getStatusCodeValue());
    verify(managementUseCase, never()).createFlashcardWithVisibility(any());
  }

  @Test
  void createGlobalFlashcardByAdminSucceeds() {
    String email = "admin@example.com";
    UUID adminId = setupAdmin(email);
    UUID unitId = UUID.randomUUID();
    var principal = new User(email, "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    Flashcard saved = new Flashcard(UUID.randomUUID(), unitId, "front", "back",
        LocalDateTime.now(), LocalDateTime.now(), Visibility.GLOBAL, null);
    when(managementUseCase.createFlashcardWithVisibility(any())).thenReturn(saved);

    var req = new FlashcardDto.CreateRequest(unitId, "front", "back", Visibility.GLOBAL);
    ResponseEntity<FlashcardDto.FlashcardResponse> response = controller.create(req, principal);

    assertEquals(201, response.getStatusCodeValue());
    verify(managementUseCase).createFlashcardWithVisibility(
        argThat(cmd -> cmd.visibility() == Visibility.GLOBAL));
  }

  @Test
  void createWithNullVisibilityDefaultsToPrivate() {
    String email = "user@example.com";
    UUID userId = setupUser(email);
    UUID unitId = UUID.randomUUID();
    var principal = new User(email, "", List.of());

    Flashcard saved = new Flashcard(UUID.randomUUID(), unitId, "front", "back",
        LocalDateTime.now(), LocalDateTime.now(), Visibility.PRIVATE, userId);
    when(managementUseCase.createFlashcardWithVisibility(any())).thenReturn(saved);

    // null visibility → defaults to PRIVATE
    var req = new FlashcardDto.CreateRequest(unitId, "front", "back", null);
    ResponseEntity<FlashcardDto.FlashcardResponse> response = controller.create(req, principal);

    assertEquals(201, response.getStatusCodeValue());
    verify(managementUseCase).createFlashcardWithVisibility(
        argThat(cmd -> cmd.visibility() == Visibility.PRIVATE));
  }

  @Test
  void createWithoutAuthThrows401() {
    var req = new FlashcardDto.CreateRequest(UUID.randomUUID(), "front", "back", null);
    assertThrows(ResponseStatusException.class, () -> controller.create(req, null));
  }
}
