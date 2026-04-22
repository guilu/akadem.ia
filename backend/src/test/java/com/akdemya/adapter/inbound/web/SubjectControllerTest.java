package com.akdemya.adapter.inbound.web;

import com.akdemya.application.service.ContentManagement;
import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.model.Subject;
import com.akdemya.domain.model.Visibility;
import com.akdemya.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SubjectControllerTest {

  private final ContentManagement contentService = mock(ContentManagement.class);
  private final UserRepository userRepo = mock(UserRepository.class);

  private final SubjectController controller = new SubjectController(contentService, userRepo);

  private User userPrincipal(String email) {
    UUID userId = UUID.randomUUID();
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

  // --- POST /api/subjects ---

  @Test
  void createUnauthenticatedReturns401() {
    SubjectController.CreateSubjectRequest req =
        new SubjectController.CreateSubjectRequest("Math", "desc", null);
    ResponseEntity<Subject> response = controller.create(req, null);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verify(contentService, never()).createSubject(any());
  }

  @Test
  void createWithNullVisibilityDefaultsToPrivate() {
    String email = "user@example.com";
    User principal = userPrincipal(email);
    Subject privateSubject = Subject.createPrivate("Math", "desc", UUID.randomUUID());
    when(contentService.createSubject(any())).thenReturn(privateSubject);

    SubjectController.CreateSubjectRequest req =
        new SubjectController.CreateSubjectRequest("Math", "desc", null);
    ResponseEntity<Subject> response = controller.create(req, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    // null visibility should default to PRIVATE, so createPrivate path is taken
    verify(contentService).createSubject(any());
  }

  // --- DELETE /api/subjects/{id} ---

  @Test
  void deleteUnauthenticatedReturns401() {
    ResponseEntity<?> response = controller.delete(UUID.randomUUID(), null);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verify(contentService, never()).deleteSubjectIfAuthorized(any(), any(), anyBoolean());
  }

  @Test
  void deleteGlobalSubjectByNonAdminReturns403() {
    String email = "user@example.com";
    User principal = userPrincipal(email);
    UUID subjectId = UUID.randomUUID();
    doThrow(new AccessDeniedException("Only admins can delete GLOBAL subjects"))
        .when(contentService).deleteSubjectIfAuthorized(eq(subjectId), any(), eq(false));

    ResponseEntity<?> response = controller.delete(subjectId, principal);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  @Test
  void deleteByAdminReturns200() {
    String email = "admin@example.com";
    User principal = adminPrincipal(email);
    UUID subjectId = UUID.randomUUID();
    doNothing().when(contentService).deleteSubjectIfAuthorized(eq(subjectId), any(), eq(true));

    ResponseEntity<?> response = controller.delete(subjectId, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(contentService).deleteSubjectIfAuthorized(eq(subjectId), any(), eq(true));
  }
}
