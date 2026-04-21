package com.akdemya.adapter.inbound.web;

import com.akdemya.application.service.ContentManagement;
import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.model.Syllabus;
import com.akdemya.domain.model.Visibility;
import com.akdemya.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ManageSyllabusControllerTest {

  private final ContentManagement contentService = mock(ContentManagement.class);
  private final UserRepository userRepo = mock(UserRepository.class);

  private final ManageSyllabusController controller = new ManageSyllabusController(contentService, userRepo);

  private UUID userId = UUID.randomUUID();

  private User userPrincipal(String email) {
    when(userRepo.findByEmail(email))
        .thenReturn(Optional.of(new AppUser(userId, email, "", "STUDENT", null, null, null)));
    return new User(email, "", List.of());
  }

  private User adminPrincipal(String email) {
    UUID adminId = UUID.randomUUID();
    when(userRepo.findByEmail(email))
        .thenReturn(Optional.of(new AppUser(adminId, email, "", "ADMIN", null, null, null)));
    return new User(email, "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
  }

  // --- GET /api/manage/syllabuses ---

  @Test
  void nullPrincipalReturnsUnauthorized() {
    ResponseEntity<?> response = controller.list(null);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void authenticatedUserCanListSyllabuses() {
    User principal = userPrincipal("user@example.com");
    Syllabus s = Syllabus.createPrivate("My Syllabus", "desc", userId);
    when(contentService.getPrivateSyllabuses(userId)).thenReturn(List.of(s));

    ResponseEntity<List<ManageSyllabusController.ManageSyllabusResponse>> response = controller.list(principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
    assertEquals(Visibility.PRIVATE.name(), response.getBody().get(0).visibility());
    verify(contentService).getPrivateSyllabuses(userId);
    verify(contentService, never()).getVisibleSyllabuses(any());
    assertTrue(response.getBody().get(0).isEditable());
  }

  @Test
  void adminListsOnlyGlobalSyllabusesAsReadOnly() {
    User principal = adminPrincipal("admin@example.com");
    Syllabus global = Syllabus.createGlobal("Global", "desc");
    when(contentService.getVisibleSyllabuses(null)).thenReturn(List.of(global));

    ResponseEntity<List<ManageSyllabusController.ManageSyllabusResponse>> response = controller.list(principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(contentService).getVisibleSyllabuses(null);
    verify(contentService, never()).getPrivateSyllabuses(any());
    assertEquals(1, response.getBody().size());
    assertEquals(Visibility.GLOBAL.name(), response.getBody().get(0).visibility());
    assertFalse(response.getBody().get(0).isEditable());
  }

  // --- POST /api/manage/syllabuses ---

  @Test
  void createWithNullPrincipalReturnsUnauthorized() {
    var req = new ManageSyllabusController.SyllabusRequest("Test", "desc", "PRIVATE");
    ResponseEntity<?> response = controller.create(req, null);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verify(contentService, never()).createSyllabus(any());
  }

  @Test
  void createWithNullNameReturnsBadRequest() {
    User principal = userPrincipal("user@example.com");
    var req = new ManageSyllabusController.SyllabusRequest(null, "desc", "PRIVATE");
    ResponseEntity<?> response = controller.create(req, principal);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    verify(contentService, never()).createSyllabus(any());
  }

  @Test
  void createWithBlankNameReturnsBadRequest() {
    User principal = userPrincipal("user@example.com");
    var req = new ManageSyllabusController.SyllabusRequest("  ", "desc", "PRIVATE");
    ResponseEntity<?> response = controller.create(req, principal);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    verify(contentService, never()).createSyllabus(any());
  }

  @Test
  void nonAdminCreatingGlobalReturns403() {
    User principal = userPrincipal("user@example.com");
    var req = new ManageSyllabusController.SyllabusRequest("Global", "desc", "GLOBAL");
    ResponseEntity<?> response = controller.create(req, principal);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    verify(contentService, never()).createSyllabus(any());
  }

  @Test
  void userCanCreatePrivateSyllabus() {
    User principal = userPrincipal("user@example.com");
    Syllabus saved = Syllabus.createPrivate("My Syllabus", "desc", userId);
    when(contentService.createSyllabus(any())).thenReturn(saved);

    var req = new ManageSyllabusController.SyllabusRequest("My Syllabus", "desc", "PRIVATE");
    ResponseEntity<?> response = controller.create(req, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(contentService).createSyllabus(any());
  }

  @Test
  void adminCanCreateGlobalSyllabus() {
    User principal = adminPrincipal("admin@example.com");
    Syllabus saved = Syllabus.createGlobal("Global Syllabus", "desc");
    when(contentService.createSyllabus(any())).thenReturn(saved);

    var req = new ManageSyllabusController.SyllabusRequest("Global Syllabus", "desc", "PRIVATE");
    ResponseEntity<?> response = controller.create(req, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(contentService).createSyllabus(argThat(s -> s.getVisibility() == Visibility.GLOBAL && s.getOwnerId() == null));
    var body = assertInstanceOf(ManageSyllabusController.ManageSyllabusResponse.class, response.getBody());
    assertFalse(body.isEditable());
  }

  // --- PUT /api/manage/syllabuses/{id} ---

  @Test
  void updateWithNullPrincipalReturnsUnauthorized() {
    var req = new ManageSyllabusController.SyllabusRequest("Name", "desc", null);
    ResponseEntity<?> response = controller.update(UUID.randomUUID(), req, null);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void updateNonExistentSyllabusReturnsNotFound() {
    User principal = userPrincipal("user@example.com");
    UUID syllabusId = UUID.randomUUID();
    when(contentService.getSyllabusById(syllabusId)).thenReturn(Optional.empty());

    var req = new ManageSyllabusController.SyllabusRequest("New Name", "desc", null);
    ResponseEntity<?> response = controller.update(syllabusId, req, principal);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void nonAdminCannotUpdateGlobalSyllabus() {
    User principal = userPrincipal("user@example.com");
    UUID syllabusId = UUID.randomUUID();
    Syllabus global = new Syllabus(syllabusId, "Global", "desc", Visibility.GLOBAL, null);
    when(contentService.getSyllabusById(syllabusId)).thenReturn(Optional.of(global));

    var req = new ManageSyllabusController.SyllabusRequest("New Name", "desc", null);
    ResponseEntity<?> response = controller.update(syllabusId, req, principal);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  @Test
  void nonAdminCanUpdateOwnPrivateSyllabus() {
    User principal = userPrincipal("user@example.com");
    UUID syllabusId = UUID.randomUUID();
    Syllabus existing = new Syllabus(syllabusId, "Old Name", "desc", Visibility.PRIVATE, userId);
    Syllabus updated = new Syllabus(syllabusId, "New Name", "desc", Visibility.PRIVATE, userId);
    when(contentService.getSyllabusById(syllabusId)).thenReturn(Optional.of(existing));
    when(contentService.createSyllabus(any())).thenReturn(updated);

    var req = new ManageSyllabusController.SyllabusRequest("New Name", "desc", null);
    ResponseEntity<?> response = controller.update(syllabusId, req, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void adminCannotUpdateGlobalSyllabusFromManage() {
    User principal = adminPrincipal("admin@example.com");
    UUID syllabusId = UUID.randomUUID();
    Syllabus existing = new Syllabus(syllabusId, "Global", "desc", Visibility.GLOBAL, null);
    when(contentService.getSyllabusById(syllabusId)).thenReturn(Optional.of(existing));

    var req = new ManageSyllabusController.SyllabusRequest("New Name", "desc", null);
    ResponseEntity<?> response = controller.update(syllabusId, req, principal);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    verify(contentService, never()).createSyllabus(any());
  }

  @Test
  void updateWithBlankNameReturnsBadRequest() {
    User principal = userPrincipal("user@example.com");
    UUID syllabusId = UUID.randomUUID();
    Syllabus existing = new Syllabus(syllabusId, "Old Name", "desc", Visibility.PRIVATE, userId);
    when(contentService.getSyllabusById(syllabusId)).thenReturn(Optional.of(existing));

    var req = new ManageSyllabusController.SyllabusRequest("  ", "desc", null);
    ResponseEntity<?> response = controller.update(syllabusId, req, principal);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  // --- DELETE /api/manage/syllabuses/{id} ---

  @Test
  void deleteWithNullPrincipalReturnsUnauthorized() {
    ResponseEntity<?> response = controller.delete(UUID.randomUUID(), null);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void deleteByAdminReturnsForbiddenForGlobalSyllabus() {
    User principal = adminPrincipal("admin@example.com");
    UUID syllabusId = UUID.randomUUID();
    Syllabus existing = new Syllabus(syllabusId, "Global", "desc", Visibility.GLOBAL, null);
    when(contentService.getSyllabusById(syllabusId)).thenReturn(Optional.of(existing));

    ResponseEntity<?> response = controller.delete(syllabusId, principal);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    verify(contentService, never()).deleteSyllabus(any(), any(), anyBoolean());
  }

  @Test
  void deleteNonExistentSyllabusReturnsNotFound() {
    User principal = userPrincipal("user@example.com");
    UUID syllabusId = UUID.randomUUID();
    when(contentService.getSyllabusById(syllabusId)).thenReturn(Optional.empty());

    ResponseEntity<?> response = controller.delete(syllabusId, principal);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void deleteSyllabusWithSubjectsReturns409() {
    User principal = userPrincipal("user@example.com");
    UUID syllabusId = UUID.randomUUID();
    Syllabus existing = new Syllabus(syllabusId, "Mine", "desc", Visibility.PRIVATE, userId);
    when(contentService.getSyllabusById(syllabusId)).thenReturn(Optional.of(existing));
    doThrow(new IllegalStateException("Cannot delete syllabus with linked subjects"))
        .when(contentService).deleteSyllabus(eq(syllabusId), any(), eq(false));

    ResponseEntity<?> response = controller.delete(syllabusId, principal);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
  }
}
