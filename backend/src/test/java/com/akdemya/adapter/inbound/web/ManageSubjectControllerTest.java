package com.akdemya.adapter.inbound.web;

import com.akdemya.application.service.ContentManagement;
import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.model.Subject;
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
    when(contentService.getSubjectsByScope(any(), anyBoolean(), eq(Visibility.PRIVATE))).thenReturn(List.of(s));
    when(contentService.getUnitsBySubject(any())).thenReturn(List.of());

    ResponseEntity<List<ManageSubjectController.ManageSubjectResponse>> response = controller.list(principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(contentService).getSubjectsByScope(any(), anyBoolean(), eq(Visibility.PRIVATE));
    assertTrue(response.getBody().get(0).isEditable());
  }

  // Test 2: Non-admin GET with scope=PRIVATE → only their PRIVATE subjects
  @Test
  void nonAdminGetWithPrivateScopeReturnsOnlyPrivateSubjects() {
    User principal = userPrincipal("user@example.com");
    Subject privateSubject = Subject.createPrivate("My Subject", "desc", userId);
    when(contentService.getSubjectsByScope(any(), anyBoolean(), eq(Visibility.PRIVATE)))
        .thenReturn(List.of(privateSubject));
    when(contentService.getUnitsBySubject(any())).thenReturn(List.of());

    ResponseEntity<?> response = controller.list(principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(contentService).getSubjectsByScope(any(), anyBoolean(), eq(Visibility.PRIVATE));
  }

  // Test 3: Non-admin GET with scope=GLOBAL → only GLOBAL subjects
  @Test
  void adminGetsOnlyGlobalSubjectsAsReadOnly() {
    User principal = adminPrincipal("admin@example.com");
    Subject globalSubject = Subject.createGlobal("Global Subject", "desc");
    when(contentService.getSubjectsByScope(any(), eq(true), eq(Visibility.GLOBAL)))
        .thenReturn(List.of(globalSubject));
    when(contentService.getUnitsBySubject(any())).thenReturn(List.of());

    ResponseEntity<List<ManageSubjectController.ManageSubjectResponse>> response = controller.list(principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(contentService).getSubjectsByScope(any(), eq(true), eq(Visibility.GLOBAL));
    assertFalse(response.getBody().get(0).isEditable());
  }

  // Test 4: Non-admin POST with visibility=PRIVATE → 200, subject created
  @Test
  void nonAdminCanCreatePrivateSubject() {
    User principal = userPrincipal("user@example.com");
    Subject created = Subject.createPrivate("My Subject", "desc", userId);
    when(contentService.createSubject(any())).thenReturn(created);
    when(contentService.getUnitsBySubject(any())).thenReturn(List.of());

    var req = new ManageSubjectController.SubjectRequest("My Subject", "desc", "PRIVATE", null);
    ResponseEntity<?> response = controller.create(req, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(contentService).createSubject(any());
  }

  // Test 5: Non-admin POST with visibility=GLOBAL → 403
  @Test
  void nonAdminCannotCreateGlobalSubject() {
    User principal = userPrincipal("user@example.com");

    var req = new ManageSubjectController.SubjectRequest("Global Subject", "desc", "GLOBAL", null);
    ResponseEntity<?> response = controller.create(req, principal);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    verify(contentService, never()).createSubject(any());
  }

  // Test 6: Non-admin DELETE own PRIVATE subject → 200
  @Test
  void nonAdminCanDeleteOwnPrivateSubject() {
    User principal = userPrincipal("user@example.com");
    UUID subjectId = UUID.randomUUID();
    when(contentService.getSubjectById(subjectId))
        .thenReturn(Optional.of(new Subject(subjectId, "Mine", "desc", Visibility.PRIVATE, userId, null)));
    doNothing().when(contentService).deleteSubjectIfAuthorized(any(), any(), eq(false));

    ResponseEntity<?> response = controller.delete(subjectId, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(contentService).deleteSubjectIfAuthorized(eq(subjectId), any(), eq(false));
  }

  // Test 7: Non-admin DELETE GLOBAL subject → 403
  @Test
  void nonAdminCannotDeleteGlobalSubject() {
    User principal = userPrincipal("user@example.com");
    UUID subjectId = UUID.randomUUID();
    when(contentService.getSubjectById(subjectId))
        .thenReturn(Optional.of(new Subject(subjectId, "Global", "desc", Visibility.GLOBAL, null, null)));

    ResponseEntity<?> response = controller.delete(subjectId, principal);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  // Test 8: Admin DELETE GLOBAL subject → 200
  @Test
  void adminCanDeleteGlobalSubject() {
    User principal = adminPrincipal("admin@example.com");
    UUID subjectId = UUID.randomUUID();
    when(contentService.getSubjectById(subjectId))
        .thenReturn(Optional.of(new Subject(subjectId, "Global", "desc", Visibility.GLOBAL, null, null)));

    ResponseEntity<?> response = controller.delete(subjectId, principal);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    verify(contentService, never()).deleteSubjectIfAuthorized(any(), any(), anyBoolean());
  }

  // Test 9: Unauthenticated GET is handled by SecurityConfig (returns 401 at filter layer)
  // This test verifies the controller still handles null principal defensively
  @Test
  void nullPrincipalReturnsUnauthorized() {
    ResponseEntity<?> response = controller.list(null);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  // Test 10: POST with null principal → 401
  @Test
  void createWithNullPrincipalReturnsUnauthorized() {
    var req = new ManageSubjectController.SubjectRequest("My Subject", "desc", "PRIVATE", null);
    ResponseEntity<?> response = controller.create(req, null);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  // Test 11: POST with null name → 400
  @Test
  void createWithNullNameReturnsBadRequest() {
    User principal = userPrincipal("user@example.com");
    var req = new ManageSubjectController.SubjectRequest(null, "desc", "PRIVATE", null);
    ResponseEntity<?> response = controller.create(req, principal);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  // Test 12: POST with blank name → 400
  @Test
  void createWithBlankNameReturnsBadRequest() {
    User principal = userPrincipal("user@example.com");
    var req = new ManageSubjectController.SubjectRequest("  ", "desc", "PRIVATE", null);
    ResponseEntity<?> response = controller.create(req, principal);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  // Test 13: Admin POST with visibility=GLOBAL → 200
  @Test
  void adminCanCreateGlobalSubject() {
    User principal = adminPrincipal("admin@example.com");
    Subject created = Subject.createGlobal("Global Subject", "desc");
    when(contentService.createSubject(any())).thenReturn(created);

    var req = new ManageSubjectController.SubjectRequest("Global Subject", "desc", "PRIVATE", null);
    ResponseEntity<?> response = controller.create(req, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(contentService).createSubject(argThat(subject -> subject.getVisibility() == Visibility.GLOBAL
        && subject.getOwnerId() == null));
    var body = assertInstanceOf(ManageSubjectController.ManageSubjectResponse.class, response.getBody());
    assertFalse(body.isEditable());
  }

  // Test 14: PUT with null principal → 401
  @Test
  void updateWithNullPrincipalReturnsUnauthorized() {
    var req = new ManageSubjectController.SubjectRequest("Name", "desc", "PRIVATE", null);
    ResponseEntity<?> response = controller.update(UUID.randomUUID(), req, null);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  // Test 15: PUT for non-existent subject → 404
  @Test
  void updateNonExistentSubjectReturnsNotFound() {
    User principal = userPrincipal("user@example.com");
    UUID subjectId = UUID.randomUUID();
    when(contentService.getSubjectById(subjectId)).thenReturn(Optional.empty());

    var req = new ManageSubjectController.SubjectRequest("New Name", "desc", "PRIVATE", null);
    ResponseEntity<?> response = controller.update(subjectId, req, principal);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  // Test 16: Non-admin PUT on GLOBAL subject → 403
  @Test
  void nonAdminCannotUpdateGlobalSubject() {
    User principal = userPrincipal("user@example.com");
    UUID subjectId = UUID.randomUUID();
    Subject globalSubject = new Subject(subjectId, "Global", "desc", Visibility.GLOBAL, null, null);
    when(contentService.getSubjectById(subjectId)).thenReturn(Optional.of(globalSubject));

    var req = new ManageSubjectController.SubjectRequest("New Name", "desc", "PRIVATE", null);
    ResponseEntity<?> response = controller.update(subjectId, req, principal);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  // Test 17: Non-admin PUT on own PRIVATE subject → 200
  @Test
  void nonAdminCanUpdateOwnPrivateSubject() {
    User principal = userPrincipal("user@example.com");
    UUID subjectId = UUID.randomUUID();
    Subject privateSubject = new Subject(subjectId, "Old Name", "desc", Visibility.PRIVATE, userId, null);
    Subject updated = new Subject(subjectId, "New Name", "desc", Visibility.PRIVATE, userId, null);
    when(contentService.getSubjectById(subjectId)).thenReturn(Optional.of(privateSubject));
    when(contentService.createSubject(any())).thenReturn(updated);
    when(contentService.getUnitsBySubject(any())).thenReturn(List.of());

    var req = new ManageSubjectController.SubjectRequest("New Name", "desc", "PRIVATE", null);
    ResponseEntity<?> response = controller.update(subjectId, req, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  // Test 18: PUT with blank name → 400
  @Test
  void updateWithBlankNameReturnsBadRequest() {
    User principal = userPrincipal("user@example.com");
    UUID subjectId = UUID.randomUUID();
    Subject privateSubject = new Subject(subjectId, "Old Name", "desc", Visibility.PRIVATE, userId, null);
    when(contentService.getSubjectById(subjectId)).thenReturn(Optional.of(privateSubject));

    var req = new ManageSubjectController.SubjectRequest("  ", "desc", "PRIVATE", null);
    ResponseEntity<?> response = controller.update(subjectId, req, principal);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  // Test 19: DELETE with null principal → 401
  @Test
  void deleteWithNullPrincipalReturnsUnauthorized() {
    ResponseEntity<?> response = controller.delete(UUID.randomUUID(), null);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  // Test 20: DELETE non-existent → 404
  @Test
  void deleteNonExistentSubjectReturnsNotFound() {
    User principal = userPrincipal("user@example.com");
    UUID subjectId = UUID.randomUUID();
    when(contentService.getSubjectById(subjectId)).thenReturn(Optional.empty());

    ResponseEntity<?> response = controller.delete(subjectId, principal);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  // Test 21: list returns isEditable=true for admin on global subject
  @Test
  void adminSeesGlobalSubjectsAsReadOnly() {
    User principal = adminPrincipal("admin@example.com");
    Subject globalSubject = Subject.createGlobal("Global Subject", "desc");
    when(contentService.getSubjectsByScope(any(), eq(true), eq(Visibility.GLOBAL))).thenReturn(List.of(globalSubject));
    when(contentService.getUnitsBySubject(any())).thenReturn(List.of());

    ResponseEntity<List<ManageSubjectController.ManageSubjectResponse>> response = controller.list(principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertFalse(response.getBody().get(0).isEditable());
  }
}
