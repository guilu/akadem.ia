package com.akdemya.adapter.inbound.web;

import com.akdemya.application.service.ContentManagement;
import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.model.Subject;
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

class ManageSubjectControllerTest {

  private final ContentManagement contentService = mock(ContentManagement.class);
  private final UserRepository userRepo = mock(UserRepository.class);

  private final ManageSubjectController controller =
      new ManageSubjectController(contentService, userRepo);

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

  // Test 1: Authenticated non-admin can GET /api/manage/subjects → 200
  @Test
  void authenticatedUserCanListSubjects() {
    User principal = userPrincipal("user@example.com");
    Subject s = Subject.createPrivate("My Subject", "desc", userId);
    when(contentService.getSubjectsByScope(any(), anyBoolean(), isNull())).thenReturn(List.of(s));
    when(contentService.getUnitsBySubject(any())).thenReturn(List.of());

    ResponseEntity<?> response = controller.list(null, principal);

    assertEquals(200, response.getStatusCodeValue());
    verify(contentService).getSubjectsByScope(any(), anyBoolean(), isNull());
  }

  // Test 2: Non-admin GET with scope=PRIVATE → only their PRIVATE subjects
  @Test
  void nonAdminGetWithPrivateScopeReturnsOnlyPrivateSubjects() {
    User principal = userPrincipal("user@example.com");
    Subject privateSubject = Subject.createPrivate("My Subject", "desc", userId);
    when(contentService.getSubjectsByScope(any(), anyBoolean(), eq(Visibility.PRIVATE)))
        .thenReturn(List.of(privateSubject));
    when(contentService.getUnitsBySubject(any())).thenReturn(List.of());

    ResponseEntity<?> response = controller.list("PRIVATE", principal);

    assertEquals(200, response.getStatusCodeValue());
    verify(contentService).getSubjectsByScope(any(), anyBoolean(), eq(Visibility.PRIVATE));
  }

  // Test 3: Non-admin GET with scope=GLOBAL → only GLOBAL subjects
  @Test
  void nonAdminGetWithGlobalScopeReturnsOnlyGlobalSubjects() {
    User principal = userPrincipal("user@example.com");
    Subject globalSubject = Subject.createGlobal("Global Subject", "desc");
    when(contentService.getSubjectsByScope(any(), anyBoolean(), eq(Visibility.GLOBAL)))
        .thenReturn(List.of(globalSubject));
    when(contentService.getUnitsBySubject(any())).thenReturn(List.of());

    ResponseEntity<?> response = controller.list("GLOBAL", principal);

    assertEquals(200, response.getStatusCodeValue());
    verify(contentService).getSubjectsByScope(any(), anyBoolean(), eq(Visibility.GLOBAL));
  }

  // Test 4: Non-admin POST with visibility=PRIVATE → 200, subject created
  @Test
  void nonAdminCanCreatePrivateSubject() {
    User principal = userPrincipal("user@example.com");
    Subject created = Subject.createPrivate("My Subject", "desc", userId);
    when(contentService.createSubject(any())).thenReturn(created);
    when(contentService.getUnitsBySubject(any())).thenReturn(List.of());

    var req = new ManageSubjectController.SubjectRequest("My Subject", "desc", "PRIVATE");
    ResponseEntity<?> response = controller.create(req, principal);

    assertEquals(200, response.getStatusCodeValue());
    verify(contentService).createSubject(any());
  }

  // Test 5: Non-admin POST with visibility=GLOBAL → 403
  @Test
  void nonAdminCannotCreateGlobalSubject() {
    User principal = userPrincipal("user@example.com");

    var req = new ManageSubjectController.SubjectRequest("Global Subject", "desc", "GLOBAL");
    ResponseEntity<?> response = controller.create(req, principal);

    assertEquals(403, response.getStatusCodeValue());
    verify(contentService, never()).createSubject(any());
  }

  // Test 6: Non-admin DELETE own PRIVATE subject → 200
  @Test
  void nonAdminCanDeleteOwnPrivateSubject() {
    User principal = userPrincipal("user@example.com");
    UUID subjectId = UUID.randomUUID();
    doNothing().when(contentService).deleteSubjectIfAuthorized(any(), any(), eq(false));

    ResponseEntity<?> response = controller.delete(subjectId, principal);

    assertEquals(200, response.getStatusCodeValue());
    verify(contentService).deleteSubjectIfAuthorized(eq(subjectId), any(), eq(false));
  }

  // Test 7: Non-admin DELETE GLOBAL subject → 403
  @Test
  void nonAdminCannotDeleteGlobalSubject() {
    User principal = userPrincipal("user@example.com");
    UUID subjectId = UUID.randomUUID();
    doThrow(new AccessDeniedException("Only admins can delete GLOBAL subjects"))
        .when(contentService).deleteSubjectIfAuthorized(any(), any(), eq(false));

    ResponseEntity<?> response = controller.delete(subjectId, principal);

    assertEquals(403, response.getStatusCodeValue());
  }

  // Test 8: Admin DELETE GLOBAL subject → 200
  @Test
  void adminCanDeleteGlobalSubject() {
    User principal = adminPrincipal("admin@example.com");
    UUID subjectId = UUID.randomUUID();
    doNothing().when(contentService).deleteSubjectIfAuthorized(any(), any(), eq(true));

    ResponseEntity<?> response = controller.delete(subjectId, principal);

    assertEquals(200, response.getStatusCodeValue());
    verify(contentService).deleteSubjectIfAuthorized(eq(subjectId), any(), eq(true));
  }

  // Test 9: Unauthenticated GET is handled by SecurityConfig (returns 401 at filter layer)
  // This test verifies the controller still handles null principal defensively
  @Test
  void nullPrincipalReturnsUnauthorized() {
    ResponseEntity<?> response = controller.list(null, null);
    assertEquals(401, response.getStatusCodeValue());
  }
}
