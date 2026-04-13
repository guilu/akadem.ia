package com.akdemya.adapter.inbound.web;

import com.akdemya.application.service.ContentManagement;
import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.model.Syllabus;
import com.akdemya.domain.model.Visibility;
import com.akdemya.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
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
    assertEquals(401, response.getStatusCode().value());
  }

  @Test
  void authenticatedUserCanListSyllabuses() {
    User principal = userPrincipal("user@example.com");
    Syllabus s = Syllabus.createPrivate("My Syllabus", "desc", userId);
    when(contentService.getVisibleSyllabuses(any())).thenReturn(List.of(s));

    ResponseEntity<?> response = controller.list(principal);

    assertEquals(200, response.getStatusCode().value());
    verify(contentService).getVisibleSyllabuses(any());
  }

  // --- POST /api/manage/syllabuses ---

  @Test
  void createWithNullPrincipalReturnsUnauthorized() {
    var req = new ManageSyllabusController.SyllabusRequest("Test", "desc", "PRIVATE");
    ResponseEntity<?> response = controller.create(req, null);
    assertEquals(401, response.getStatusCode().value());
    verify(contentService, never()).createSyllabus(any());
  }

  @Test
  void createWithNullNameReturnsBadRequest() {
    User principal = userPrincipal("user@example.com");
    var req = new ManageSyllabusController.SyllabusRequest(null, "desc", "PRIVATE");
    ResponseEntity<?> response = controller.create(req, principal);
    assertEquals(400, response.getStatusCode().value());
    verify(contentService, never()).createSyllabus(any());
  }

  @Test
  void createWithBlankNameReturnsBadRequest() {
    User principal = userPrincipal("user@example.com");
    var req = new ManageSyllabusController.SyllabusRequest("  ", "desc", "PRIVATE");
    ResponseEntity<?> response = controller.create(req, principal);
    assertEquals(400, response.getStatusCode().value());
    verify(contentService, never()).createSyllabus(any());
  }

  @Test
  void nonAdminCreatingGlobalReturns403() {
    User principal = userPrincipal("user@example.com");
    var req = new ManageSyllabusController.SyllabusRequest("Global", "desc", "GLOBAL");
    ResponseEntity<?> response = controller.create(req, principal);
    assertEquals(403, response.getStatusCode().value());
    verify(contentService, never()).createSyllabus(any());
  }

  @Test
  void userCanCreatePrivateSyllabus() {
    User principal = userPrincipal("user@example.com");
    Syllabus saved = Syllabus.createPrivate("My Syllabus", "desc", userId);
    when(contentService.createSyllabus(any())).thenReturn(saved);

    var req = new ManageSyllabusController.SyllabusRequest("My Syllabus", "desc", "PRIVATE");
    ResponseEntity<?> response = controller.create(req, principal);

    assertEquals(200, response.getStatusCode().value());
    verify(contentService).createSyllabus(any());
  }

  @Test
  void adminCanCreateGlobalSyllabus() {
    User principal = adminPrincipal("admin@example.com");
    Syllabus saved = Syllabus.createGlobal("Global Syllabus", "desc");
    when(contentService.createSyllabus(any())).thenReturn(saved);

    var req = new ManageSyllabusController.SyllabusRequest("Global Syllabus", "desc", "GLOBAL");
    ResponseEntity<?> response = controller.create(req, principal);

    assertEquals(200, response.getStatusCode().value());
    verify(contentService).createSyllabus(any());
  }

  // --- PUT /api/manage/syllabuses/{id} ---

  @Test
  void updateWithNullPrincipalReturnsUnauthorized() {
    var req = new ManageSyllabusController.SyllabusRequest("Name", "desc", null);
    ResponseEntity<?> response = controller.update(UUID.randomUUID(), req, null);
    assertEquals(401, response.getStatusCode().value());
  }

  @Test
  void updateNonExistentSyllabusReturnsNotFound() {
    User principal = userPrincipal("user@example.com");
    UUID syllabusId = UUID.randomUUID();
    when(contentService.getSyllabusById(syllabusId)).thenReturn(Optional.empty());

    var req = new ManageSyllabusController.SyllabusRequest("New Name", "desc", null);
    ResponseEntity<?> response = controller.update(syllabusId, req, principal);

    assertEquals(404, response.getStatusCode().value());
  }

  @Test
  void nonAdminCannotUpdateGlobalSyllabus() {
    User principal = userPrincipal("user@example.com");
    UUID syllabusId = UUID.randomUUID();
    Syllabus global = new Syllabus(syllabusId, "Global", "desc", Visibility.GLOBAL, null);
    when(contentService.getSyllabusById(syllabusId)).thenReturn(Optional.of(global));

    var req = new ManageSyllabusController.SyllabusRequest("New Name", "desc", null);
    ResponseEntity<?> response = controller.update(syllabusId, req, principal);

    assertEquals(403, response.getStatusCode().value());
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

    assertEquals(200, response.getStatusCode().value());
  }

  @Test
  void updateWithBlankNameReturnsBadRequest() {
    User principal = userPrincipal("user@example.com");
    UUID syllabusId = UUID.randomUUID();
    Syllabus existing = new Syllabus(syllabusId, "Old Name", "desc", Visibility.PRIVATE, userId);
    when(contentService.getSyllabusById(syllabusId)).thenReturn(Optional.of(existing));

    var req = new ManageSyllabusController.SyllabusRequest("  ", "desc", null);
    ResponseEntity<?> response = controller.update(syllabusId, req, principal);

    assertEquals(400, response.getStatusCode().value());
  }

  // --- DELETE /api/manage/syllabuses/{id} ---

  @Test
  void deleteWithNullPrincipalReturnsUnauthorized() {
    ResponseEntity<?> response = controller.delete(UUID.randomUUID(), null);
    assertEquals(401, response.getStatusCode().value());
  }

  @Test
  void deleteByAdminReturns200() {
    User principal = adminPrincipal("admin@example.com");
    UUID syllabusId = UUID.randomUUID();
    doNothing().when(contentService).deleteSyllabus(eq(syllabusId), any(), eq(true));

    ResponseEntity<?> response = controller.delete(syllabusId, principal);

    assertEquals(200, response.getStatusCode().value());
  }

  @Test
  void deleteNonExistentSyllabusReturnsNotFound() {
    User principal = userPrincipal("user@example.com");
    UUID syllabusId = UUID.randomUUID();
    doThrow(new IllegalArgumentException("Not found"))
        .when(contentService).deleteSyllabus(any(), any(), anyBoolean());

    ResponseEntity<?> response = controller.delete(syllabusId, principal);

    assertEquals(404, response.getStatusCode().value());
  }

  @Test
  void deleteSyllabusWithSubjectsReturns409() {
    User principal = adminPrincipal("admin@example.com");
    UUID syllabusId = UUID.randomUUID();
    doThrow(new IllegalStateException("Cannot delete syllabus with linked subjects"))
        .when(contentService).deleteSyllabus(eq(syllabusId), any(), eq(true));

    ResponseEntity<?> response = controller.delete(syllabusId, principal);

    assertEquals(409, response.getStatusCode().value());
  }
}
