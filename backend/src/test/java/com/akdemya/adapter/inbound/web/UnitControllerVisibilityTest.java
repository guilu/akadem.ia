package com.akdemya.adapter.inbound.web;

import com.akdemya.application.service.ContentManagement;
import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.model.Unit;
import com.akdemya.domain.model.Visibility;
import com.akdemya.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UnitControllerVisibilityTest {

  private final ContentManagement contentService = mock(ContentManagement.class);
  private final UserRepository userRepo = mock(UserRepository.class);

  private final UnitController controller = new UnitController(contentService, userRepo);

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

  // --- Listing ---

  @Test
  void unauthenticatedGetReturnsAllUnits() {
    UUID subjectId = UUID.randomUUID();
    Unit globalUnit = Unit.createGlobal(subjectId, "Algebra", "desc", 1);
    when(contentService.getUnitsBySubject(subjectId)).thenReturn(List.of(globalUnit));

    List<Unit> result = controller.getBySubject(subjectId, null);

    assertEquals(1, result.size());
    verify(contentService).getUnitsBySubject(subjectId);
    verify(contentService, never()).getVisibleUnitsBySubject(any(), any());
  }

  @Test
  void authenticatedGetReturnsVisibleUnits() {
    String email = "user@example.com";
    UUID userId = setupUser(email);
    UUID subjectId = UUID.randomUUID();
    var principal = new User(email, "", List.of());

    Unit globalUnit = Unit.createGlobal(subjectId, "Algebra", "desc", 1);
    Unit privateOwned = Unit.createPrivate(subjectId, "My Algebra", "desc", 1, userId);
    when(contentService.getVisibleUnitsBySubject(subjectId, userId))
        .thenReturn(List.of(globalUnit, privateOwned));

    List<Unit> result = controller.getBySubject(subjectId, principal);

    assertEquals(2, result.size());
    verify(contentService).getVisibleUnitsBySubject(subjectId, userId);
    verify(contentService, never()).getUnitsBySubject(any());
  }

  // --- Creation ---

  @Test
  void unauthenticatedCreateReturns401() {
    var req = new UnitController.CreateUnitRequest(UUID.randomUUID(), "Algebra", "desc", 1, null);
    ResponseEntity<Unit> response = controller.create(req, null);
    assertEquals(401, response.getStatusCodeValue());
    verify(contentService, never()).createUnit(any());
  }

  @Test
  void createPrivateUnitByAuthenticatedUserReturns200() {
    String email = "user@example.com";
    UUID userId = setupUser(email);
    UUID subjectId = UUID.randomUUID();
    var principal = new User(email, "", List.of());

    Unit privateUnit = Unit.createPrivate(subjectId, "My Algebra", "desc", 1, userId);
    when(contentService.createUnit(any())).thenReturn(privateUnit);

    var req = new UnitController.CreateUnitRequest(subjectId, "My Algebra", "desc", 1, Visibility.PRIVATE);
    ResponseEntity<Unit> response = controller.create(req, principal);

    assertEquals(200, response.getStatusCodeValue());
    assertNotNull(response.getBody());
    assertEquals(Visibility.PRIVATE, response.getBody().getVisibility());
  }

  @Test
  void createGlobalUnitByNonAdminReturns403() {
    String email = "user@example.com";
    setupUser(email);
    var principal = new User(email, "", List.of());

    var req = new UnitController.CreateUnitRequest(UUID.randomUUID(), "Global Algebra", "desc", 1, Visibility.GLOBAL);
    ResponseEntity<Unit> response = controller.create(req, principal);

    assertEquals(403, response.getStatusCodeValue());
    verify(contentService, never()).createUnit(any());
  }

  @Test
  void createGlobalUnitByAdminReturns200() {
    String email = "admin@example.com";
    UUID adminId = setupAdmin(email);
    UUID subjectId = UUID.randomUUID();
    var principal = new User(email, "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    Unit globalUnit = Unit.createGlobal(subjectId, "Global Algebra", "desc", 1);
    when(contentService.createUnit(any())).thenReturn(globalUnit);

    var req = new UnitController.CreateUnitRequest(subjectId, "Global Algebra", "desc", 1, Visibility.GLOBAL);
    ResponseEntity<Unit> response = controller.create(req, principal);

    assertEquals(200, response.getStatusCodeValue());
    verify(contentService).createUnit(any());
  }

  @Test
  void createWithNullVisibilityDefaultsToPrivate() {
    String email = "user@example.com";
    UUID userId = setupUser(email);
    UUID subjectId = UUID.randomUUID();
    var principal = new User(email, "", List.of());

    Unit privateUnit = Unit.createPrivate(subjectId, "My Unit", "desc", 0, userId);
    when(contentService.createUnit(any())).thenReturn(privateUnit);

    // null visibility → defaults to PRIVATE
    var req = new UnitController.CreateUnitRequest(subjectId, "My Unit", "desc", 0, null);
    ResponseEntity<Unit> response = controller.create(req, principal);

    assertEquals(200, response.getStatusCodeValue());
    verify(contentService).createUnit(argThat(u -> u.getVisibility() == Visibility.PRIVATE));
  }
}
