package com.akdemya.adapter.inbound.web;

import com.akdemya.application.service.ContentManagement;
import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.model.Question;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class QuestionControllerVisibilityTest {

  private final ContentManagement contentService = mock(ContentManagement.class);
  private final UserRepository userRepo = mock(UserRepository.class);

  private final QuestionController controller = new QuestionController(contentService, userRepo);

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
  void unauthenticatedGetReturnsAllQuestionsForUnit() {
    UUID unitId = UUID.randomUUID();
    Question globalQ = Question.createGlobal(unitId, "Q?", "exp", Question.Difficulty.EASY);
    when(contentService.getQuestionsByUnit(unitId)).thenReturn(List.of(globalQ));
    when(contentService.getAnswersByQuestion(any())).thenReturn(List.of());

    var result = controller.getByUnit(unitId, null);

    assertEquals(1, result.size());
    verify(contentService).getQuestionsByUnit(unitId);
    verify(contentService, never()).getVisibleQuestionsByUnit(any(), any());
  }

  @Test
  void authenticatedGetReturnsVisibleQuestions() {
    String email = "user@example.com";
    UUID userId = setupUser(email);
    UUID unitId = UUID.randomUUID();
    var principal = new User(email, "", List.of());

    Question globalQ = Question.createGlobal(unitId, "Global Q?", "exp", Question.Difficulty.EASY);
    Question privateQ = Question.createPrivate(unitId, "Private Q?", "exp", Question.Difficulty.EASY, userId);
    when(contentService.getVisibleQuestionsByUnit(unitId, userId))
        .thenReturn(List.of(globalQ, privateQ));
    when(contentService.getAnswersByQuestion(any())).thenReturn(List.of());

    var result = controller.getByUnit(unitId, principal);

    assertEquals(2, result.size());
    verify(contentService).getVisibleQuestionsByUnit(unitId, userId);
    verify(contentService, never()).getQuestionsByUnit(any());
  }

  // --- Creation ---

  @Test
  void unauthenticatedCreateReturns401() {
    var req = new QuestionController.CreateQuestionRequest(
        UUID.randomUUID(), "Q?", "exp", Question.Difficulty.EASY, null);
    ResponseEntity<Question> response = controller.create(req, null);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verify(contentService, never()).createQuestion(any());
  }

  @Test
  void createPrivateQuestionByAuthenticatedUserReturns200() {
    String email = "user@example.com";
    UUID userId = setupUser(email);
    UUID unitId = UUID.randomUUID();
    var principal = new User(email, "", List.of());

    Question privateQ = Question.createPrivate(unitId, "My Q?", "exp", Question.Difficulty.EASY, userId);
    when(contentService.createQuestion(any())).thenReturn(privateQ);

    var req = new QuestionController.CreateQuestionRequest(
        unitId, "My Q?", "exp", Question.Difficulty.EASY, Visibility.PRIVATE);
    ResponseEntity<Question> response = controller.create(req, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(Visibility.PRIVATE, response.getBody().getVisibility());
  }

  @Test
  void createGlobalQuestionByNonAdminReturns403() {
    String email = "user@example.com";
    setupUser(email);
    var principal = new User(email, "", List.of());

    var req = new QuestionController.CreateQuestionRequest(
        UUID.randomUUID(), "Global Q?", "exp", Question.Difficulty.EASY, Visibility.GLOBAL);
    ResponseEntity<Question> response = controller.create(req, principal);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    verify(contentService, never()).createQuestion(any());
  }

  @Test
  void createGlobalQuestionByAdminReturns200() {
    String email = "admin@example.com";
    UUID adminId = setupAdmin(email);
    UUID unitId = UUID.randomUUID();
    var principal = new User(email, "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    Question globalQ = Question.createGlobal(unitId, "Global Q?", "exp", Question.Difficulty.MEDIUM);
    when(contentService.createQuestion(any())).thenReturn(globalQ);

    var req = new QuestionController.CreateQuestionRequest(
        unitId, "Global Q?", "exp", Question.Difficulty.MEDIUM, Visibility.GLOBAL);
    ResponseEntity<Question> response = controller.create(req, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(contentService).createQuestion(any());
  }

  @Test
  void createWithNullVisibilityDefaultsToPrivate() {
    String email = "user@example.com";
    UUID userId = setupUser(email);
    UUID unitId = UUID.randomUUID();
    var principal = new User(email, "", List.of());

    Question privateQ = Question.createPrivate(unitId, "My Q?", "exp", Question.Difficulty.EASY, userId);
    when(contentService.createQuestion(any())).thenReturn(privateQ);

    // null visibility → defaults to PRIVATE
    var req = new QuestionController.CreateQuestionRequest(
        unitId, "My Q?", "exp", Question.Difficulty.EASY, null);
    ResponseEntity<Question> response = controller.create(req, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(contentService).createQuestion(argThat(q -> q.getVisibility() == Visibility.PRIVATE));
  }
}
