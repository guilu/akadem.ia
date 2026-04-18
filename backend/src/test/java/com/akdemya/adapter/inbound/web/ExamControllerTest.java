package com.akdemya.adapter.inbound.web;

import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.port.in.ExamUseCase;
import com.akdemya.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ExamControllerTest {

  private final ExamUseCase examUseCase = mock(ExamUseCase.class);
  private final PrincipalResolver principalResolver = mock(PrincipalResolver.class);

  private final ExamController controller = new ExamController(examUseCase, principalResolver);

  private final UUID userId = UUID.randomUUID();

  private User userPrincipal(String email) {
    when(principalResolver.requireUserId(any())).thenReturn(userId);
    return new User(email, "", List.of());
  }

  // --- startRandom ---

  @Test
  void startRandomWithNullPrincipalReturnsUnauthorized() {
    var req = new ExamController.StartRandomRequest(UUID.randomUUID(), 10, 30, "EASY");
    ResponseEntity<?> response = controller.startRandom(req, null);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void startRandomAuthenticatedReturnsOk() {
    User principal = userPrincipal("user@example.com");
    UUID subjectId = UUID.randomUUID();
    UUID attemptId = UUID.randomUUID();
    var startResponse = new ExamUseCase.StartResponse(attemptId, 1800, List.of());
    when(examUseCase.startRandomExam(any())).thenReturn(startResponse);

    var req = new ExamController.StartRandomRequest(subjectId, 10, 30, "EASY");
    ResponseEntity<?> response = controller.startRandom(req, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(examUseCase).startRandomExam(any(ExamUseCase.StartRandomCommand.class));
  }

  // --- start ---

  @Test
  void startWithNullPrincipalReturnsUnauthorized() {
    var req = new ExamController.StartRequest(Map.of(), 30, "EASY");
    ResponseEntity<?> response = controller.start(req, null);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void startAuthenticatedReturnsOk() {
    User principal = userPrincipal("user@example.com");
    UUID attemptId = UUID.randomUUID();
    var startResponse = new ExamUseCase.StartResponse(attemptId, 1800, List.of());
    when(examUseCase.startExam(any())).thenReturn(startResponse);

    UUID unitId = UUID.randomUUID();
    var req = new ExamController.StartRequest(Map.of(unitId, 5), 30, "MEDIUM");
    ResponseEntity<?> response = controller.start(req, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(examUseCase).startExam(any(ExamUseCase.StartCommand.class));
  }

  // --- submit ---

  @Test
  void submitWithNullPrincipalReturnsUnauthorized() {
    UUID attemptId = UUID.randomUUID();
    var cmd = new ExamUseCase.SubmitCommand(attemptId, Map.of());
    ResponseEntity<?> response = controller.submit(attemptId, cmd, null);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void submitAuthenticatedReturnsOk() {
    User principal = userPrincipal("user@example.com");
    UUID attemptId = UUID.randomUUID();
    var result = new ExamUseCase.SubmitResult(10, 8, 2, 0, 8, 80.0);
    when(examUseCase.submitExam(any(), anyString())).thenReturn(result);

    var cmd = new ExamUseCase.SubmitCommand(attemptId, Map.of());
    ResponseEntity<?> response = controller.submit(attemptId, cmd, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void submitNotFoundReturns404() {
    User principal = userPrincipal("user@example.com");
    UUID attemptId = UUID.randomUUID();
    when(examUseCase.submitExam(any(), anyString()))
        .thenThrow(new NoSuchElementException("Attempt not found"));

    var cmd = new ExamUseCase.SubmitCommand(attemptId, Map.of());
    ResponseEntity<?> response = controller.submit(attemptId, cmd, principal);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void submitForbiddenReturns403() {
    User principal = userPrincipal("user@example.com");
    UUID attemptId = UUID.randomUUID();
    when(examUseCase.submitExam(any(), anyString()))
        .thenThrow(new SecurityException("Forbidden"));

    var cmd = new ExamUseCase.SubmitCommand(attemptId, Map.of());
    ResponseEntity<?> response = controller.submit(attemptId, cmd, principal);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  // --- updateAnswer ---

  @Test
  void updateAnswerWithNullPrincipalReturnsUnauthorized() {
    UUID attemptId = UUID.randomUUID();
    UUID questionId = UUID.randomUUID();
    var req = new ExamController.UpdateAnswerRequest(UUID.randomUUID());
    ResponseEntity<?> response = controller.updateAnswer(attemptId, questionId, req, null);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void updateAnswerWithNullSelectedAnswerReturnsBadRequest() {
    User principal = userPrincipal("user@example.com");
    UUID attemptId = UUID.randomUUID();
    UUID questionId = UUID.randomUUID();
    var req = new ExamController.UpdateAnswerRequest(null);
    ResponseEntity<?> response = controller.updateAnswer(attemptId, questionId, req, principal);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void updateAnswerAuthenticatedReturnsNoContent() {
    User principal = userPrincipal("user@example.com");
    UUID attemptId = UUID.randomUUID();
    UUID questionId = UUID.randomUUID();
    UUID answerId = UUID.randomUUID();
    doNothing().when(examUseCase).updateAnswer(any(), anyString());

    var req = new ExamController.UpdateAnswerRequest(answerId);
    ResponseEntity<?> response = controller.updateAnswer(attemptId, questionId, req, principal);

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }

  @Test
  void updateAnswerNotFoundReturns404() {
    User principal = userPrincipal("user@example.com");
    UUID attemptId = UUID.randomUUID();
    UUID questionId = UUID.randomUUID();
    UUID answerId = UUID.randomUUID();
    doThrow(new NoSuchElementException("Not found"))
        .when(examUseCase).updateAnswer(any(), anyString());

    var req = new ExamController.UpdateAnswerRequest(answerId);
    ResponseEntity<?> response = controller.updateAnswer(attemptId, questionId, req, principal);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void updateAnswerForbiddenReturns403() {
    User principal = userPrincipal("user@example.com");
    UUID attemptId = UUID.randomUUID();
    UUID questionId = UUID.randomUUID();
    UUID answerId = UUID.randomUUID();
    doThrow(new SecurityException("Forbidden"))
        .when(examUseCase).updateAnswer(any(), anyString());

    var req = new ExamController.UpdateAnswerRequest(answerId);
    ResponseEntity<?> response = controller.updateAnswer(attemptId, questionId, req, principal);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  // --- getAttempt ---

  @Test
  void getAttemptWithNullPrincipalReturnsUnauthorized() {
    ResponseEntity<?> response = controller.getAttempt(UUID.randomUUID(), null);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void getAttemptAuthenticatedReturnsOk() {
    User principal = userPrincipal("user@example.com");
    UUID attemptId = UUID.randomUUID();
    var attemptResponse = new ExamUseCase.AttemptResponse(attemptId, 1800, List.of(), 0, false);
    when(examUseCase.getAttempt(eq(attemptId), anyString())).thenReturn(attemptResponse);

    ResponseEntity<?> response = controller.getAttempt(attemptId, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void getAttemptNotFoundReturns404() {
    User principal = userPrincipal("user@example.com");
    UUID attemptId = UUID.randomUUID();
    when(examUseCase.getAttempt(eq(attemptId), anyString()))
        .thenThrow(new NoSuchElementException("Not found"));

    ResponseEntity<?> response = controller.getAttempt(attemptId, principal);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void getAttemptForbiddenReturns403() {
    User principal = userPrincipal("user@example.com");
    UUID attemptId = UUID.randomUUID();
    when(examUseCase.getAttempt(eq(attemptId), anyString()))
        .thenThrow(new SecurityException("Forbidden"));

    ResponseEntity<?> response = controller.getAttempt(attemptId, principal);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  // --- listAttempts ---

  @Test
  void listAttemptsWithNullPrincipalReturnsUnauthorized() {
    ResponseEntity<?> response = controller.listAttempts(null);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void listAttemptsAuthenticatedReturnsOk() {
    User principal = userPrincipal("user@example.com");
    when(examUseCase.listAttempts(anyString())).thenReturn(List.of());

    ResponseEntity<?> response = controller.listAttempts(principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(examUseCase).listAttempts("user@example.com");
  }
}
