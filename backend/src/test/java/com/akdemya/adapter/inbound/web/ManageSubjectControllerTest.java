package com.akdemya.adapter.inbound.web;

import com.akdemya.application.service.ContentManagement;
import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.model.Subject;
import com.akdemya.domain.model.Syllabus;
import com.akdemya.domain.model.Visibility;
import com.akdemya.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
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
    assertTrue(response.getBody().get(0).isEditable());
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
    doNothing().when(contentService).deleteSubjectIfAuthorized(any(), any(), anyBoolean());

    ResponseEntity<?> response = controller.delete(subjectId, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(contentService).deleteSubjectIfAuthorized(eq(subjectId), any(), eq(true));
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
    assertTrue(body.isEditable());
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
    assertTrue(response.getBody().get(0).isEditable());
  }

  // Test 22: importSubjectsValidCsvCreatesSubjects — admin, 3-row CSV → 200, created=3, errors empty
  @Test
  void importSubjectsValidCsvCreatesSubjects() throws Exception {
    UUID syllabusId = UUID.randomUUID();
    User principal = adminPrincipal("admin@example.com");
    String csv = "name,description\nTema 1,Desc 1\nTema 2,Desc 2\nTema 3,Desc 3\n";
    var file = mock(MultipartFile.class);
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn((long) csv.length());
    when(file.getBytes()).thenReturn(csv.getBytes());

    when(contentService.getSubjectsByScope(any(), eq(true), eq(Visibility.GLOBAL))).thenReturn(List.of());
    Subject created = Subject.createGlobal("Tema 1", "Desc 1", syllabusId);
    when(contentService.createSubject(any())).thenReturn(created);

    ResponseEntity<Object> response = controller.importSubjects(file, "csv", syllabusId, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertEquals(3, body.get("created"));
    assertTrue(((List<?>) body.get("errors")).isEmpty());
    verify(contentService, times(3)).createSubject(any());
  }

  // Test 23: importSubjectsNonAdminEmptyFileReturnsBadRequest — non-admin, empty file → 400
  // Per design: non-admins import PRIVATE subjects (no blanket 403). Guard fires on empty file.
  @Test
  void importSubjectsNonAdminEmptyFileReturnsBadRequest() throws Exception {
    UUID syllabusId = UUID.randomUUID();
    User principal = userPrincipal("user@example.com");
    var file = mock(MultipartFile.class);
    when(file.isEmpty()).thenReturn(true);

    ResponseEntity<Object> response = controller.importSubjects(file, "csv", syllabusId, principal);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    verify(contentService, never()).createSubject(any());
  }

  // Test 24: importSubjectsExceedingRowLimitReturnsBadRequest — admin, 501 rows → 400 row_limit_exceeded
  @Test
  void importSubjectsExceedingRowLimitReturnsBadRequest() throws Exception {
    UUID syllabusId = UUID.randomUUID();
    User principal = adminPrincipal("admin@example.com");
    StringBuilder sb = new StringBuilder("name,description\n");
    for (int i = 1; i <= 501; i++) {
      sb.append("Tema ").append(i).append(",Desc ").append(i).append("\n");
    }
    String csv = sb.toString();
    var file = mock(MultipartFile.class);
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn((long) csv.length());
    when(file.getBytes()).thenReturn(csv.getBytes());
    when(contentService.getSubjectsByScope(any(), eq(true), eq(Visibility.GLOBAL))).thenReturn(List.of());

    ResponseEntity<Object> response = controller.importSubjects(file, "csv", syllabusId, principal);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertEquals("row_limit_exceeded", body.get("error"));
    verify(contentService, never()).createSubject(any());
  }

  // Test 25: importSubjectsPartialErrorReportsRowErrors — 3-row CSV where row 2 has blank name
  @Test
  void importSubjectsPartialErrorReportsRowErrors() throws Exception {
    UUID syllabusId = UUID.randomUUID();
    User principal = adminPrincipal("admin@example.com");
    String csv = "name,description\nTema 1,Desc 1\n,Desc 2\nTema 3,Desc 3\n";
    var file = mock(MultipartFile.class);
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn((long) csv.length());
    when(file.getBytes()).thenReturn(csv.getBytes());

    when(contentService.getSubjectsByScope(any(), eq(true), eq(Visibility.GLOBAL))).thenReturn(List.of());
    Subject created = Subject.createGlobal("Tema 1", "Desc 1", syllabusId);
    when(contentService.createSubject(any())).thenReturn(created);

    ResponseEntity<Object> response = controller.importSubjects(file, "csv", syllabusId, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertEquals(2, body.get("created"));
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> errors = (List<Map<String, Object>>) body.get("errors");
    assertEquals(1, errors.size());
    assertEquals(2, errors.get(0).get("row"));
    verify(contentService, times(2)).createSubject(any());
  }

  // Test 26: importSubjectsDuplicateNameReportsRowError — existing "Tema A" → duplicate row error
  @Test
  void importSubjectsDuplicateNameReportsRowError() throws Exception {
    UUID syllabusId = UUID.randomUUID();
    User principal = adminPrincipal("admin@example.com");
    String csv = "name,description\nTema A,Desc\n";
    var file = mock(MultipartFile.class);
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn((long) csv.length());
    when(file.getBytes()).thenReturn(csv.getBytes());

    Subject existing = Subject.createGlobal("Tema A", "Existing desc", syllabusId);
    when(contentService.getSubjectsByScope(any(), eq(true), eq(Visibility.GLOBAL))).thenReturn(List.of(existing));

    ResponseEntity<Object> response = controller.importSubjects(file, "csv", syllabusId, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertEquals(0, body.get("created"));
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> errors = (List<Map<String, Object>>) body.get("errors");
    assertEquals(1, errors.size());
    assertEquals(1, errors.get(0).get("row"));
    verify(contentService, never()).createSubject(any());
  }

  // Test 27: importSubjects_nonOwnerSyllabus_returnsForbidden
  // Non-admin user tries to import into a syllabus that belongs to another user → 403
  @Test
  void importSubjects_nonOwnerSyllabus_returnsForbidden() throws Exception {
    UUID syllabusId = UUID.randomUUID();
    UUID otherOwnerId = UUID.randomUUID(); // different from userId
    User principal = userPrincipal("user@example.com");

    var file = mock(MultipartFile.class);
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn(30L);

    Syllabus otherUserSyllabus = Syllabus.createPrivate("Other Syllabus", "desc", otherOwnerId);
    when(contentService.getSyllabusById(syllabusId)).thenReturn(Optional.of(otherUserSyllabus));

    ResponseEntity<Object> response = controller.importSubjects(file, "csv", syllabusId, principal);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertEquals("not_authorized", body.get("error"));
    verify(contentService, never()).createSubject(any());
  }
}
