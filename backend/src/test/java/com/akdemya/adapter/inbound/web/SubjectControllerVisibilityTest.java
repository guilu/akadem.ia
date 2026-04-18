package com.akdemya.adapter.inbound.web;

import com.akdemya.application.service.ContentManagement;
import com.akdemya.domain.model.Subject;
import com.akdemya.domain.model.Visibility;
import com.akdemya.domain.port.out.UserRepository;
import com.akdemya.domain.model.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SubjectControllerVisibilityTest {

  private final ContentManagement contentService = mock(ContentManagement.class);
  private final UserRepository userRepo = mock(UserRepository.class);

  private final SubjectController controller = new SubjectController(contentService, userRepo);

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

  @Test
  void unauthenticatedGetReturnsOnlyGlobalSubjects() {
    Subject global = Subject.createGlobal("Math", "desc");
    when(contentService.getAllSubjects()).thenReturn(List.of(global));

    List<Subject> result = controller.getAll(null);

    assertEquals(1, result.size());
    assertEquals(Visibility.GLOBAL, result.get(0).getVisibility());
    verify(contentService).getAllSubjects();
    verify(contentService, never()).getVisibleSubjects(any());
  }

  @Test
  void authenticatedGetReturnsGlobalAndPrivateSubjects() {
    String email = "user@example.com";
    UUID userId = setupUser(email);
    var principal = new User(email, "", List.of());

    Subject global = Subject.createGlobal("Math", "desc");
    Subject privateOwned = Subject.createPrivate("My Math", "desc", userId);
    when(contentService.getVisibleSubjects(userId)).thenReturn(List.of(global, privateOwned));

    List<Subject> result = controller.getAll(principal);

    assertEquals(2, result.size());
    verify(contentService).getVisibleSubjects(userId);
    verify(contentService, never()).getAllSubjects();
  }

  @Test
  void createPrivateSubjectByAuthenticatedUserReturns200() {
    String email = "user@example.com";
    UUID userId = setupUser(email);
    var principal = new User(email, "", List.of());

    Subject privateSubject = Subject.createPrivate("My Math", "desc", userId);
    when(contentService.createSubject(any())).thenReturn(privateSubject);

    SubjectController.CreateSubjectRequest req =
        new SubjectController.CreateSubjectRequest("My Math", "desc", Visibility.PRIVATE);
    ResponseEntity<Subject> response = controller.create(req, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(Visibility.PRIVATE, response.getBody().getVisibility());
  }

  @Test
  void createGlobalSubjectByNonAdminReturns403() {
    String email = "user@example.com";
    setupUser(email);
    var principal = new User(email, "", List.of());

    SubjectController.CreateSubjectRequest req =
        new SubjectController.CreateSubjectRequest("Global Math", "desc", Visibility.GLOBAL);
    ResponseEntity<Subject> response = controller.create(req, principal);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    verify(contentService, never()).createSubject(any());
  }

  @Test
  void createGlobalSubjectByAdminReturns200() {
    String email = "admin@example.com";
    UUID adminId = setupAdmin(email);
    var principal = new User(email, "", List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")));

    Subject global = Subject.createGlobal("Global Math", "desc");
    when(contentService.createSubject(any())).thenReturn(global);

    SubjectController.CreateSubjectRequest req =
        new SubjectController.CreateSubjectRequest("Global Math", "desc", Visibility.GLOBAL);
    ResponseEntity<Subject> response = controller.create(req, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(contentService).createSubject(any());
  }
}
