package com.akdemya.adapter.inbound.web;

import com.akdemya.application.service.ContentManagement;
import com.akdemya.domain.model.Answer;
import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.model.Question;
import com.akdemya.domain.model.Unit;
import com.akdemya.domain.model.Visibility;
import com.akdemya.domain.port.out.AnswerRepository;
import com.akdemya.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

class ManageQuestionControllerTest {

  private final ContentManagement contentService = mock(ContentManagement.class);
  private final AnswerRepository answerRepo = mock(AnswerRepository.class);
  private final UserRepository userRepo = mock(UserRepository.class);

  private final ManageQuestionController controller =
      new ManageQuestionController(contentService, answerRepo, userRepo);

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

  private List<ManageQuestionController.AnswerRequest> validAnswers() {
    return List.of(
        new ManageQuestionController.AnswerRequest("Answer A", true),
        new ManageQuestionController.AnswerRequest("Answer B", false),
        new ManageQuestionController.AnswerRequest("Answer C", false),
        new ManageQuestionController.AnswerRequest("Answer D", false)
    );
  }

  @Test
  void authenticatedUserCanListQuestions() {
    User principal = userPrincipal("user@example.com");
    UUID unitId = UUID.randomUUID();
    var emptyPage = new PageImpl<Question>(List.of(), PageRequest.of(0, 10), 0);
    when(contentService.getQuestionsByScope(eq(unitId), any(), anyBoolean(), isNull(), anyInt(), anyInt()))
        .thenReturn(emptyPage);

    ResponseEntity<?> response = controller.list(unitId, null, 1, 10, principal);

    assertEquals(200, response.getStatusCodeValue());
  }

  @Test
  void nonAdminCanCreatePrivateQuestion() {
    User principal = userPrincipal("user@example.com");
    UUID unitId = UUID.randomUUID();
    Unit unit = Unit.createPrivate(UUID.randomUUID(), "My Unit", "desc", 1, userId);
    when(contentService.getUnitsBySubject(any())).thenReturn(List.of(unit));
    // Mock unit lookup for the controller
    Question created = Question.createPrivate(unitId, "Question?", null, Question.Difficulty.EASY, userId);
    when(contentService.createQuestion(any())).thenReturn(created);
    when(answerRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(answerRepo.findByQuestionId(any())).thenReturn(List.of());

    var req = new ManageQuestionController.QuestionRequest(unitId, "Question?", null,
        Question.Difficulty.EASY, validAnswers(), "PRIVATE");
    ResponseEntity<?> response = controller.create(req, principal);

    assertEquals(200, response.getStatusCodeValue());
    verify(contentService).createQuestion(any());
  }

  @Test
  void nonAdminCannotCreateGlobalQuestion() {
    User principal = userPrincipal("user@example.com");
    UUID unitId = UUID.randomUUID();

    var req = new ManageQuestionController.QuestionRequest(unitId, "Question?", null,
        Question.Difficulty.EASY, validAnswers(), "GLOBAL");
    ResponseEntity<?> response = controller.create(req, principal);

    assertEquals(403, response.getStatusCodeValue());
    verify(contentService, never()).createQuestion(any());
  }

  @Test
  void nonAdminCanDeleteOwnPrivateQuestion() {
    User principal = userPrincipal("user@example.com");
    UUID questionId = UUID.randomUUID();
    doNothing().when(contentService).deleteQuestionIfAuthorized(any(), any(), eq(false));

    ResponseEntity<?> response = controller.delete(questionId, principal);

    assertEquals(200, response.getStatusCodeValue());
    verify(contentService).deleteQuestionIfAuthorized(eq(questionId), any(), eq(false));
  }

  @Test
  void nonAdminCannotDeleteGlobalQuestion() {
    User principal = userPrincipal("user@example.com");
    UUID questionId = UUID.randomUUID();
    doThrow(new AccessDeniedException("Only admins can delete GLOBAL questions"))
        .when(contentService).deleteQuestionIfAuthorized(any(), any(), eq(false));

    ResponseEntity<?> response = controller.delete(questionId, principal);

    assertEquals(403, response.getStatusCodeValue());
  }

  @Test
  void adminCanDeleteGlobalQuestion() {
    User principal = adminPrincipal("admin@example.com");
    UUID questionId = UUID.randomUUID();
    doNothing().when(contentService).deleteQuestionIfAuthorized(any(), any(), eq(true));

    ResponseEntity<?> response = controller.delete(questionId, principal);

    assertEquals(200, response.getStatusCodeValue());
    verify(contentService).deleteQuestionIfAuthorized(eq(questionId), any(), eq(true));
  }

  @Test
  void nullPrincipalReturnsUnauthorized() {
    ResponseEntity<?> response = controller.list(UUID.randomUUID(), null, 1, 10, null);
    assertEquals(401, response.getStatusCodeValue());
  }

  @Test
  void createWithNullUnitIdReturnsBadRequest() {
    User principal = userPrincipal("user@example.com");

    var req = new ManageQuestionController.QuestionRequest(null, "Question?", null,
        Question.Difficulty.EASY, validAnswers(), "PRIVATE");
    ResponseEntity<?> response = controller.create(req, principal);

    assertEquals(400, response.getStatusCodeValue());
  }

  @Test
  void createWithBlankTextReturnsBadRequest() {
    User principal = userPrincipal("user@example.com");
    UUID unitId = UUID.randomUUID();

    var req = new ManageQuestionController.QuestionRequest(unitId, "  ", null,
        Question.Difficulty.EASY, validAnswers(), "PRIVATE");
    ResponseEntity<?> response = controller.create(req, principal);

    assertEquals(400, response.getStatusCodeValue());
  }
}
