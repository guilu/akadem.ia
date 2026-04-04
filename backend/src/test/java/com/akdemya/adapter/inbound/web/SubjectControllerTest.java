package com.akdemya.adapter.inbound.web;

import com.akdemya.application.service.ContentManagement;
import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.model.Subject;
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
    assertEquals(401, response.getStatusCodeValue());
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

    assertEquals(200, response.getStatusCodeValue());
    // null visibility should default to PRIVATE, so createPrivate path is taken
    verify(contentService).createSubject(any());
  }

  // --- DELETE /api/subjects/{id} ---

  @Test
  void deleteUnauthenticatedReturns401() {
    ResponseEntity<?> response = controller.delete(UUID.randomUUID(), null);
    assertEquals(401, response.getStatusCodeValue());
    verify(contentService, never()).deleteSubject(any());
  }

  @Test
  void deleteByNonAdminReturns403() {
    String email = "user@example.com";
    User principal = userPrincipal(email);

    ResponseEntity<?> response = controller.delete(UUID.randomUUID(), principal);

    assertEquals(403, response.getStatusCodeValue());
    verify(contentService, never()).deleteSubject(any());
  }

  @Test
  void deleteByAdminReturns200() {
    String email = "admin@example.com";
    User principal = adminPrincipal(email);
    UUID subjectId = UUID.randomUUID();

    ResponseEntity<?> response = controller.delete(subjectId, principal);

    assertEquals(200, response.getStatusCodeValue());
    verify(contentService).deleteSubject(subjectId);
  }
}
