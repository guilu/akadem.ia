package com.akdemya.adapter.inbound.web;

import com.akdemya.application.service.ContentManagement;
import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.model.Subject;
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

class SyllabusControllerTest {

  private final ContentManagement contentService = mock(ContentManagement.class);
  private final UserRepository userRepo = mock(UserRepository.class);

  private final SyllabusController controller = new SyllabusController(contentService, userRepo);

  private UUID userId = UUID.randomUUID();

  private User userPrincipal(String email) {
    when(userRepo.findByEmail(email))
        .thenReturn(Optional.of(new AppUser(userId, email, "", "USER", null, null, null)));
    return new User(email, "", List.of());
  }

  private User adminPrincipal(String email) {
    UUID adminId = UUID.randomUUID();
    when(userRepo.findByEmail(email))
        .thenReturn(Optional.of(new AppUser(adminId, email, "", "ADMIN", null, null, null)));
    return new User(email, "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
  }

  // --- GET /api/syllabuses ---

  @Test
  void unauthenticatedGetReturnsOnlyGlobal() {
    Syllabus global = Syllabus.createGlobal("Default", "desc");
    when(contentService.getVisibleSyllabuses(null)).thenReturn(List.of(global));

    List<SyllabusController.SyllabusResponse> result = controller.getAll(null);

    assertEquals(1, result.size());
    assertEquals("GLOBAL", result.get(0).visibility());
    verify(contentService).getVisibleSyllabuses(null);
  }

  @Test
  void authenticatedGetSeesGlobalAndOwnPrivate() {
    Syllabus global = Syllabus.createGlobal("Default", "desc");
    Syllabus priv = Syllabus.createPrivate("My Syllabus", "private", userId);
    User principal = userPrincipal("user@example.com");
    when(contentService.getVisibleSyllabuses(userId)).thenReturn(List.of(global, priv));

    List<SyllabusController.SyllabusResponse> result = controller.getAll(principal);

    assertEquals(2, result.size());
    verify(contentService).getVisibleSyllabuses(userId);
  }

  // --- POST /api/syllabuses ---

  @Test
  void createUnauthenticatedReturns401() {
    var req = new SyllabusController.CreateSyllabusRequest("Test", "desc", null);
    ResponseEntity<?> response = controller.create(req, null);
    assertEquals(401, response.getStatusCode().value());
    verify(contentService, never()).createSyllabus(any());
  }

  @Test
  void createWithNullNameReturns400() {
    User principal = userPrincipal("user@example.com");
    var req = new SyllabusController.CreateSyllabusRequest(null, "desc", null);
    ResponseEntity<?> response = controller.create(req, principal);
    assertEquals(400, response.getStatusCode().value());
    verify(contentService, never()).createSyllabus(any());
  }

  @Test
  void nonAdminCreatingGlobalReturns403() {
    User principal = userPrincipal("user@example.com");
    var req = new SyllabusController.CreateSyllabusRequest("Global", "desc", Visibility.GLOBAL);
    ResponseEntity<?> response = controller.create(req, principal);
    assertEquals(403, response.getStatusCode().value());
    verify(contentService, never()).createSyllabus(any());
  }

  @Test
  void userCanCreatePrivateSyllabus() {
    User principal = userPrincipal("user@example.com");
    Syllabus saved = Syllabus.createPrivate("My Syllabus", "desc", userId);
    when(contentService.createSyllabus(any())).thenReturn(saved);

    var req = new SyllabusController.CreateSyllabusRequest("My Syllabus", "desc", Visibility.PRIVATE);
    ResponseEntity<?> response = controller.create(req, principal);

    assertEquals(200, response.getStatusCode().value());
    verify(contentService).createSyllabus(any());
  }

  @Test
  void adminCanCreateGlobalSyllabus() {
    User principal = adminPrincipal("admin@example.com");
    Syllabus saved = Syllabus.createGlobal("Global Syllabus", "desc");
    when(contentService.createSyllabus(any())).thenReturn(saved);

    var req = new SyllabusController.CreateSyllabusRequest("Global Syllabus", "desc", Visibility.GLOBAL);
    ResponseEntity<?> response = controller.create(req, principal);

    assertEquals(200, response.getStatusCode().value());
    verify(contentService).createSyllabus(any());
  }

  // --- DELETE /api/syllabuses/{id} ---

  @Test
  void deleteUnauthenticatedReturns401() {
    ResponseEntity<?> response = controller.delete(UUID.randomUUID(), null);
    assertEquals(401, response.getStatusCode().value());
    verify(contentService, never()).deleteSyllabus(any(), any(), anyBoolean());
  }

  @Test
  void deleteSyllabusWithSubjectsReturns409() {
    User principal = userPrincipal("user@example.com");
    UUID syllabusId = UUID.randomUUID();
    doThrow(new IllegalStateException("Cannot delete syllabus with linked subjects"))
        .when(contentService).deleteSyllabus(eq(syllabusId), any(), anyBoolean());

    ResponseEntity<?> response = controller.delete(syllabusId, principal);

    assertEquals(409, response.getStatusCode().value());
  }

  @Test
  void deleteGlobalByNonAdminReturns403() {
    User principal = userPrincipal("user@example.com");
    UUID syllabusId = UUID.randomUUID();
    doThrow(new AccessDeniedException("Only admins can delete GLOBAL syllabuses"))
        .when(contentService).deleteSyllabus(eq(syllabusId), any(), eq(false));

    ResponseEntity<?> response = controller.delete(syllabusId, principal);

    assertEquals(403, response.getStatusCode().value());
  }

  @Test
  void deleteByAdminReturns200() {
    User principal = adminPrincipal("admin@example.com");
    UUID syllabusId = UUID.randomUUID();
    doNothing().when(contentService).deleteSyllabus(eq(syllabusId), any(), eq(true));

    ResponseEntity<?> response = controller.delete(syllabusId, principal);

    assertEquals(200, response.getStatusCode().value());
  }

  // --- GET /api/syllabuses/{id}/subjects ---

  @Test
  void getSubjectsForNonExistentSyllabusReturns404() {
    UUID syllabusId = UUID.randomUUID();
    when(contentService.getSyllabusById(syllabusId)).thenReturn(Optional.empty());

    ResponseEntity<?> response = controller.getSubjects(syllabusId, null);

    assertEquals(404, response.getStatusCode().value());
  }

  @Test
  void getSubjectsVisibilityScopedForAuthenticatedUser() {
    User principal = userPrincipal("user@example.com");
    UUID syllabusId = UUID.randomUUID();
    Syllabus syllabus = Syllabus.createGlobal("Default", "desc");
    Subject subject = Subject.createGlobal("Math", "desc");
    when(contentService.getSyllabusById(syllabusId)).thenReturn(Optional.of(syllabus));
    when(contentService.getSubjectsBySyllabus(eq(syllabusId), eq(userId))).thenReturn(List.of(subject));

    ResponseEntity<?> response = controller.getSubjects(syllabusId, principal);

    assertEquals(200, response.getStatusCode().value());
    verify(contentService).getSubjectsBySyllabus(eq(syllabusId), eq(userId));
  }

  @Test
  void getSubjectsUnauthenticatedReturnsOnlyGlobal() {
    UUID syllabusId = UUID.randomUUID();
    Syllabus syllabus = Syllabus.createGlobal("Default", "desc");
    Subject subject = Subject.createGlobal("Math", "desc");
    when(contentService.getSyllabusById(syllabusId)).thenReturn(Optional.of(syllabus));
    when(contentService.getSubjectsBySyllabus(eq(syllabusId), isNull())).thenReturn(List.of(subject));

    ResponseEntity<?> response = controller.getSubjects(syllabusId, null);

    assertEquals(200, response.getStatusCode().value());
    verify(contentService).getSubjectsBySyllabus(eq(syllabusId), isNull());
  }
}
