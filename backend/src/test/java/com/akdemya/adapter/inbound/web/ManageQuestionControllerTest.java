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
    when(contentService.getQuestionsByScope(eq(unitId), any(), anyBoolean(), any(), anyInt(), anyInt()))
        .thenReturn(emptyPage);

    ResponseEntity<?> response = controller.list(unitId, 1, 10, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(contentService).getQuestionsByScope(eq(unitId), any(), eq(false), eq(Visibility.PRIVATE), anyInt(), anyInt());
  }

  @Test
  void adminListsOnlyGlobalQuestionsAsReadOnly() {
    User principal = adminPrincipal("admin@example.com");
    UUID unitId = UUID.randomUUID();
    Question q = Question.createGlobal(unitId, "Global?", null, Question.Difficulty.EASY);
    var page = new PageImpl<Question>(List.of(q), PageRequest.of(0, 10), 1);
    when(contentService.getQuestionsByScope(eq(unitId), any(), eq(true), eq(Visibility.GLOBAL), anyInt(), anyInt()))
        .thenReturn(page);
    when(answerRepo.findByQuestionId(q.getId())).thenReturn(List.of());

    ResponseEntity<ManageQuestionController.PageResponse<ManageQuestionController.ManageQuestionResponse>> response =
        controller.list(unitId, 1, 10, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(contentService).getQuestionsByScope(eq(unitId), any(), eq(true), eq(Visibility.GLOBAL), anyInt(), anyInt());
    assertTrue(response.getBody().items().get(0).isEditable());
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

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(contentService).createQuestion(any());
  }

  @Test
  void nonAdminCannotCreateGlobalQuestion() {
    User principal = userPrincipal("user@example.com");
    UUID unitId = UUID.randomUUID();

    var req = new ManageQuestionController.QuestionRequest(unitId, "Question?", null,
        Question.Difficulty.EASY, validAnswers(), "GLOBAL");
    ResponseEntity<?> response = controller.create(req, principal);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    verify(contentService, never()).createQuestion(any());
  }

  @Test
  void nonAdminCanDeleteOwnPrivateQuestion() {
    User principal = userPrincipal("user@example.com");
    UUID questionId = UUID.randomUUID();
    when(contentService.getQuestionById(questionId))
        .thenReturn(Optional.of(new Question(questionId, UUID.randomUUID(), "Mine?", null,
            Question.Difficulty.EASY, Visibility.PRIVATE, userId)));
    doNothing().when(contentService).deleteQuestionIfAuthorized(any(), any(), eq(false));

    ResponseEntity<?> response = controller.delete(questionId, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(contentService).deleteQuestionIfAuthorized(eq(questionId), any(), eq(false));
  }

  @Test
  void nonAdminCannotDeleteGlobalQuestion() {
    User principal = userPrincipal("user@example.com");
    UUID questionId = UUID.randomUUID();
    when(contentService.getQuestionById(questionId))
        .thenReturn(Optional.of(new Question(questionId, UUID.randomUUID(), "Global?", null,
            Question.Difficulty.EASY, Visibility.GLOBAL, null)));

    ResponseEntity<?> response = controller.delete(questionId, principal);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  @Test
  void adminCanDeleteGlobalQuestion() {
    User principal = adminPrincipal("admin@example.com");
    UUID questionId = UUID.randomUUID();
    when(contentService.getQuestionById(questionId))
        .thenReturn(Optional.of(new Question(questionId, UUID.randomUUID(), "Global?", null,
            Question.Difficulty.EASY, Visibility.GLOBAL, null)));
    doNothing().when(contentService).deleteQuestionIfAuthorized(any(), any(), anyBoolean());

    ResponseEntity<?> response = controller.delete(questionId, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(contentService).deleteQuestionIfAuthorized(eq(questionId), any(), eq(true));
  }

  @Test
  void nullPrincipalReturnsUnauthorized() {
    ResponseEntity<?> response = controller.list(UUID.randomUUID(), 1, 10, null);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void createWithNullUnitIdReturnsBadRequest() {
    User principal = userPrincipal("user@example.com");

    var req = new ManageQuestionController.QuestionRequest(null, "Question?", null,
        Question.Difficulty.EASY, validAnswers(), "PRIVATE");
    ResponseEntity<?> response = controller.create(req, principal);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void createWithBlankTextReturnsBadRequest() {
    User principal = userPrincipal("user@example.com");
    UUID unitId = UUID.randomUUID();

    var req = new ManageQuestionController.QuestionRequest(unitId, "  ", null,
        Question.Difficulty.EASY, validAnswers(), "PRIVATE");
    ResponseEntity<?> response = controller.create(req, principal);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void createWithNullDifficultyReturnsBadRequest() {
    User principal = userPrincipal("user@example.com");
    UUID unitId = UUID.randomUUID();

    var req = new ManageQuestionController.QuestionRequest(unitId, "Question?", null,
        null, validAnswers(), "PRIVATE");
    ResponseEntity<?> response = controller.create(req, principal);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void createWithNullAnswersReturnsBadRequest() {
    User principal = userPrincipal("user@example.com");
    UUID unitId = UUID.randomUUID();

    var req = new ManageQuestionController.QuestionRequest(unitId, "Question?", null,
        Question.Difficulty.EASY, null, "PRIVATE");
    ResponseEntity<?> response = controller.create(req, principal);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void createWithWrongAnswerCountReturnsBadRequest() {
    User principal = userPrincipal("user@example.com");
    UUID unitId = UUID.randomUUID();

    var answers = List.of(
        new ManageQuestionController.AnswerRequest("A", true),
        new ManageQuestionController.AnswerRequest("B", false)
    );
    var req = new ManageQuestionController.QuestionRequest(unitId, "Question?", null,
        Question.Difficulty.EASY, answers, "PRIVATE");
    ResponseEntity<?> response = controller.create(req, principal);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void createWithMultipleCorrectAnswersReturnsBadRequest() {
    User principal = userPrincipal("user@example.com");
    UUID unitId = UUID.randomUUID();

    var answers = List.of(
        new ManageQuestionController.AnswerRequest("A", true),
        new ManageQuestionController.AnswerRequest("B", true),
        new ManageQuestionController.AnswerRequest("C", false),
        new ManageQuestionController.AnswerRequest("D", false)
    );
    var req = new ManageQuestionController.QuestionRequest(unitId, "Question?", null,
        Question.Difficulty.EASY, answers, "PRIVATE");
    ResponseEntity<?> response = controller.create(req, principal);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void createWithBlankAnswerTextReturnsBadRequest() {
    User principal = userPrincipal("user@example.com");
    UUID unitId = UUID.randomUUID();

    var answers = List.of(
        new ManageQuestionController.AnswerRequest("A", true),
        new ManageQuestionController.AnswerRequest("  ", false),
        new ManageQuestionController.AnswerRequest("C", false),
        new ManageQuestionController.AnswerRequest("D", false)
    );
    var req = new ManageQuestionController.QuestionRequest(unitId, "Question?", null,
        Question.Difficulty.EASY, answers, "PRIVATE");
    ResponseEntity<?> response = controller.create(req, principal);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void adminCanCreateGlobalQuestion() {
    User principal = adminPrincipal("admin@example.com");
    UUID unitId = UUID.randomUUID();
    Question created = Question.createGlobal(unitId, "Global Question?", null, Question.Difficulty.MEDIUM);
    when(contentService.createQuestion(any())).thenReturn(created);
    when(answerRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(answerRepo.findByQuestionId(any())).thenReturn(List.of());

    var req = new ManageQuestionController.QuestionRequest(unitId, "Global Question?", null,
        Question.Difficulty.MEDIUM, validAnswers(), "PRIVATE");
    ResponseEntity<?> response = controller.create(req, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(contentService).createQuestion(argThat(question -> question.getVisibility() == Visibility.GLOBAL
        && question.getOwnerId() == null));
    var body = assertInstanceOf(ManageQuestionController.ManageQuestionResponse.class, response.getBody());
    assertTrue(body.isEditable());
  }

  @Test
  void createWithNullPrincipalReturnsUnauthorized() {
    var req = new ManageQuestionController.QuestionRequest(UUID.randomUUID(), "Question?", null,
        Question.Difficulty.EASY, validAnswers(), "PRIVATE");
    ResponseEntity<?> response = controller.create(req, null);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void createWithNullTextReturnsBadRequest() {
    User principal = userPrincipal("user@example.com");
    UUID unitId = UUID.randomUUID();

    var req = new ManageQuestionController.QuestionRequest(unitId, null, null,
        Question.Difficulty.EASY, validAnswers(), "PRIVATE");
    ResponseEntity<?> response = controller.create(req, principal);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void deleteWithNullPrincipalReturnsUnauthorized() {
    ResponseEntity<?> response = controller.delete(UUID.randomUUID(), null);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void deleteNonExistentQuestionReturnsNotFound() {
    User principal = userPrincipal("user@example.com");
    UUID questionId = UUID.randomUUID();
    when(contentService.getQuestionById(questionId)).thenReturn(Optional.empty());

    ResponseEntity<?> response = controller.delete(questionId, principal);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void listWithGlobalScopeFiltersCorrectly() {
    User principal = userPrincipal("user@example.com");
    UUID unitId = UUID.randomUUID();
    var emptyPage = new PageImpl<Question>(List.of(), PageRequest.of(0, 10), 0);
    when(contentService.getQuestionsByScope(eq(unitId), any(), anyBoolean(), eq(Visibility.PRIVATE), anyInt(), anyInt()))
        .thenReturn(emptyPage);

    ResponseEntity<?> response = controller.list(unitId, 1, 10, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(contentService).getQuestionsByScope(eq(unitId), any(), anyBoolean(), eq(Visibility.PRIVATE), anyInt(), anyInt());
  }

  @Test
  void listWithPrivateScopeFiltersCorrectly() {
    User principal = userPrincipal("user@example.com");
    UUID unitId = UUID.randomUUID();
    var emptyPage = new PageImpl<Question>(List.of(), PageRequest.of(0, 10), 0);
    when(contentService.getQuestionsByScope(eq(unitId), any(), anyBoolean(), eq(Visibility.PRIVATE), anyInt(), anyInt()))
        .thenReturn(emptyPage);

    ResponseEntity<?> response = controller.list(unitId, 1, 10, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(contentService).getQuestionsByScope(eq(unitId), any(), anyBoolean(), eq(Visibility.PRIVATE), anyInt(), anyInt());
  }

  @Test
  void listIncludesAnswersForEachQuestion() {
    User principal = userPrincipal("user@example.com");
    UUID unitId = UUID.randomUUID();
    Question q = Question.createPrivate(unitId, "Test question", null, Question.Difficulty.EASY, userId);
    var page = new PageImpl<Question>(List.of(q), PageRequest.of(0, 10), 1);
    when(contentService.getQuestionsByScope(any(), any(), anyBoolean(), any(), anyInt(), anyInt()))
        .thenReturn(page);
    Answer answer = Answer.create(q.getId(), "Answer A", true);
    when(answerRepo.findByQuestionId(q.getId())).thenReturn(List.of(answer));

    ResponseEntity<?> response = controller.list(unitId, 1, 10, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(answerRepo).findByQuestionId(q.getId());
  }

  @Test
  void updateWithNullPrincipalReturnsUnauthorized() {
    var req = new ManageQuestionController.QuestionRequest(UUID.randomUUID(), "Q?", null,
        Question.Difficulty.EASY, validAnswers(), "PRIVATE");
    ResponseEntity<?> response = controller.update(UUID.randomUUID(), req, null);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void updateWithNullUnitIdReturnsBadRequest() {
    User principal = userPrincipal("user@example.com");
    var req = new ManageQuestionController.QuestionRequest(null, "Q?", null,
        Question.Difficulty.EASY, validAnswers(), "PRIVATE");
    ResponseEntity<?> response = controller.update(UUID.randomUUID(), req, principal);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void updateWithBlankTextReturnsBadRequest() {
    User principal = userPrincipal("user@example.com");
    UUID unitId = UUID.randomUUID();
    var req = new ManageQuestionController.QuestionRequest(unitId, "  ", null,
        Question.Difficulty.EASY, validAnswers(), "PRIVATE");
    ResponseEntity<?> response = controller.update(UUID.randomUUID(), req, principal);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void updateWithWrongAnswerCountReturnsBadRequest() {
    User principal = userPrincipal("user@example.com");
    UUID unitId = UUID.randomUUID();
    var answers = List.of(
        new ManageQuestionController.AnswerRequest("A", true),
        new ManageQuestionController.AnswerRequest("B", false)
    );
    var req = new ManageQuestionController.QuestionRequest(unitId, "Q?", null,
        Question.Difficulty.EASY, answers, "PRIVATE");
    ResponseEntity<?> response = controller.update(UUID.randomUUID(), req, principal);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void updateNonExistentQuestionReturnsNotFound() {
    User principal = userPrincipal("user@example.com");
    UUID questionId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    when(contentService.getQuestionsByUnit(unitId)).thenReturn(List.of());

    var req = new ManageQuestionController.QuestionRequest(unitId, "Q?", null,
        Question.Difficulty.EASY, validAnswers(), "PRIVATE");
    ResponseEntity<?> response = controller.update(questionId, req, principal);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void updateGlobalQuestionAsForbiddenForNonAdmin() {
    User principal = userPrincipal("user@example.com");
    UUID questionId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID otherOwnerId = UUID.randomUUID();
    Question existing = new Question(questionId, unitId, "Old?", null,
        Question.Difficulty.EASY, Visibility.GLOBAL, otherOwnerId);
    when(contentService.getQuestionsByUnit(unitId)).thenReturn(List.of(existing));

    var req = new ManageQuestionController.QuestionRequest(unitId, "New?", null,
        Question.Difficulty.EASY, validAnswers(), "PRIVATE");
    ResponseEntity<?> response = controller.update(questionId, req, principal);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  @Test
  void adminCanUpdateGlobalQuestionFromManage() {
    User principal = adminPrincipal("admin@example.com");
    UUID questionId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    Question existing = new Question(questionId, unitId, "Old?", null,
        Question.Difficulty.EASY, Visibility.GLOBAL, null);
    when(contentService.getQuestionsByUnit(unitId)).thenReturn(List.of(existing));
    when(contentService.createQuestion(any())).thenReturn(existing);
    when(answerRepo.findByQuestionId(any())).thenReturn(List.of());

    var req = new ManageQuestionController.QuestionRequest(unitId, "New?", null,
        Question.Difficulty.EASY, validAnswers(), "GLOBAL");
    ResponseEntity<?> response = controller.update(questionId, req, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(contentService).createQuestion(any());
  }

  @Test
  void nonAdminCanUpdateOwnPrivateQuestion() {
    User principal = userPrincipal("user@example.com");
    UUID questionId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    Question existing = new Question(questionId, unitId, "Old?", null,
        Question.Difficulty.EASY, Visibility.PRIVATE, userId);
    when(contentService.getQuestionsByUnit(unitId)).thenReturn(List.of(existing));
    Question updated = new Question(questionId, unitId, "New?", null,
        Question.Difficulty.EASY, Visibility.PRIVATE, userId);
    when(contentService.createQuestion(any())).thenReturn(updated);
    when(answerRepo.findByQuestionId(any())).thenReturn(List.of());

    var req = new ManageQuestionController.QuestionRequest(unitId, "New?", null,
        Question.Difficulty.EASY, validAnswers(), "PRIVATE");
    ResponseEntity<?> response = controller.update(questionId, req, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void exportWithNullPrincipalReturnsUnauthorized() {
    ResponseEntity<?> response = controller.export(null, "json", null);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void exportJsonReturnsVisibleQuestionsWhenNoUnitId_nonAdmin() {
    User principal = userPrincipal("user@example.com");
    UUID unitId = UUID.randomUUID();
    Question q = Question.createGlobal(unitId, "Q?", null, Question.Difficulty.EASY);
    when(contentService.getVisibleQuestions(userId)).thenReturn(List.of(q));
    when(answerRepo.findByQuestionId(any())).thenReturn(List.of());

    ResponseEntity<?> response = controller.export(null, "json", principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(contentService).getVisibleQuestions(userId);
    verify(contentService, never()).getAllQuestions();
  }

  @Test
  void exportJsonReturnsOnlyGlobalQuestionsWhenNoUnitId_admin() {
    User principal = adminPrincipal("admin@example.com");
    UUID unitId = UUID.randomUUID();
    Question q = Question.createGlobal(unitId, "Q?", null, Question.Difficulty.EASY);
    when(contentService.getVisibleQuestions(null)).thenReturn(List.of(q));
    when(answerRepo.findByQuestionId(any())).thenReturn(List.of());

    ResponseEntity<?> response = controller.export(null, "json", principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(contentService).getVisibleQuestions(null);
    verify(contentService, never()).getAllQuestions();
  }

  @Test
  void exportJsonByUnitIdFiltersVisibleQuestions() {
    User principal = userPrincipal("user@example.com");
    UUID unitId = UUID.randomUUID();
    Question q = Question.createGlobal(unitId, "Q?", null, Question.Difficulty.EASY);
    when(contentService.getVisibleQuestionsByUnit(unitId, userId)).thenReturn(List.of(q));
    when(answerRepo.findByQuestionId(any())).thenReturn(List.of());

    ResponseEntity<?> response = controller.export(unitId, "json", principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(contentService).getVisibleQuestionsByUnit(unitId, userId);
  }

  @Test
  void exportCsvReturnsCorrectContentType() {
    User principal = userPrincipal("user@example.com");
    UUID unitId = UUID.randomUUID();
    Question q = Question.createGlobal(unitId, "Q?", null, Question.Difficulty.EASY);
    when(contentService.getVisibleQuestionsByUnit(unitId, userId)).thenReturn(List.of(q));
    Answer a1 = Answer.create(q.getId(), "Ans1", true);
    Answer a2 = Answer.create(q.getId(), "Ans2", false);
    Answer a3 = Answer.create(q.getId(), "Ans3", false);
    Answer a4 = Answer.create(q.getId(), "Ans4", false);
    when(answerRepo.findByQuestionId(any())).thenReturn(List.of(a1, a2, a3, a4));

    ResponseEntity<?> response = controller.export(unitId, "csv", principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("text/csv", response.getHeaders().getFirst("Content-Type"));
  }

  @Test
  void importWithNullPrincipalReturnsUnauthorized() throws Exception {
    var file = mock(org.springframework.web.multipart.MultipartFile.class);
    ResponseEntity<?> response = controller.importQuestions(file, "json", null, null);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void importEmptyFileReturnsBadRequest() throws Exception {
    User principal = userPrincipal("user@example.com");
    var file = mock(org.springframework.web.multipart.MultipartFile.class);
    when(file.isEmpty()).thenReturn(true);

    ResponseEntity<?> response = controller.importQuestions(file, "json", null, principal);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void importOversizedFileReturnsBadRequest() throws Exception {
    User principal = userPrincipal("user@example.com");
    var file = mock(org.springframework.web.multipart.MultipartFile.class);
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn(20L * 1024 * 1024); // 20 MB

    ResponseEntity<?> response = controller.importQuestions(file, "json", null, principal);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void importWithInvalidContentTypeReturnsBadRequest() throws Exception {
    User principal = userPrincipal("user@example.com");
    var file = mock(org.springframework.web.multipart.MultipartFile.class);
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn(1024L);
    when(file.getContentType()).thenReturn("application/pdf");

    ResponseEntity<?> response = controller.importQuestions(file, "json", null, principal);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void importValidJsonFileCreatesQuestions() throws Exception {
    User principal = userPrincipal("user@example.com");
    UUID unitId = UUID.randomUUID();
    String json = "[{\"unitId\":\"" + unitId + "\",\"text\":\"Q1?\",\"explanation\":null," +
        "\"difficulty\":\"EASY\",\"answers\":[" +
        "{\"text\":\"A\",\"correct\":true},{\"text\":\"B\",\"correct\":false}," +
        "{\"text\":\"C\",\"correct\":false},{\"text\":\"D\",\"correct\":false}]}]";

    var file = mock(org.springframework.web.multipart.MultipartFile.class);
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn((long) json.length());
    when(file.getContentType()).thenReturn("application/json");
    when(file.getBytes()).thenReturn(json.getBytes());

    Question saved = Question.createPrivate(unitId, "Q1?", null, Question.Difficulty.EASY, userId);
    when(contentService.createQuestion(any())).thenReturn(saved);
    when(answerRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    ResponseEntity<?> response = controller.importQuestions(file, "json", null, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void importValidCsvFileCreatesQuestions() throws Exception {
    User principal = userPrincipal("user@example.com");
    UUID unitId = UUID.randomUUID();
    String csv = "unitId,text,explanation,difficulty,answer1,answer2,answer3,answer4,correctIndex\n" +
        unitId + ",Q1?,Some explanation,EASY,A,B,C,D,1\n";

    var file = mock(org.springframework.web.multipart.MultipartFile.class);
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn((long) csv.length());
    when(file.getContentType()).thenReturn("text/csv");
    when(file.getBytes()).thenReturn(csv.getBytes());

    Question saved = Question.createPrivate(unitId, "Q1?", null, Question.Difficulty.EASY, userId);
    when(contentService.createQuestion(any())).thenReturn(saved);
    when(answerRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    ResponseEntity<?> response = controller.importQuestions(file, "csv", null, principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}
