package com.akdemya.adapter.inbound.web;

import com.akdemya.application.service.ContentManagement;
import com.akdemya.domain.model.Answer;
import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.model.Question;
import com.akdemya.domain.model.Visibility;
import com.akdemya.domain.port.out.AnswerRepository;
import com.akdemya.domain.port.out.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@RestController
@RequestMapping("/api/manage/questions")
public class ManageQuestionController {

  private final ContentManagement contentService;
  private final AnswerRepository answerRepo;
  private final UserRepository userRepo;

  public ManageQuestionController(ContentManagement contentService,
                                   AnswerRepository answerRepo,
                                   UserRepository userRepo) {
    this.contentService = contentService;
    this.answerRepo = answerRepo;
    this.userRepo = userRepo;
  }

  @GetMapping
  public ResponseEntity<?> list(
      @RequestParam UUID unitId,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size,
      @AuthenticationPrincipal User principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    AppUser caller = resolveUser(principal);

    Page<Question> questions = contentService.getQuestionsByScope(
        unitId, caller.getId(), false, Visibility.PRIVATE, page - 1, size);
    var result = PageResponse.from(questions, q -> {
      List<Answer> answers = answerRepo.findByQuestionId(q.getId());
      return new ManageQuestionResponse(q.getId(), q.getUnitId(), q.getText(),
          q.getExplanation(), q.getDifficulty().name(),
          answers.stream().map(AnswerDto::from).toList(),
          q.getVisibility(), true);
    });
    return ResponseEntity.ok(result);
  }

  @PostMapping
  public ResponseEntity<?> create(@Valid @RequestBody QuestionRequest req,
                                   @AuthenticationPrincipal User principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    if (req.unitId() == null) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "unit_required"));
    }
    if (req.text() == null || req.text().trim().isEmpty()) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "text_required"));
    }
    if (req.difficulty() == null) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "difficulty_required"));
    }
    if (req.answers() == null || req.answers().size() != 4) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "answers_required"));
    }
    long correctCount = req.answers().stream().filter(AnswerRequest::correct).count();
    if (correctCount != 1) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "one_correct_required"));
    }
    if (req.answers().stream().anyMatch(a -> a.text() == null || a.text().trim().isEmpty())) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "answer_text_required"));
    }

    AppUser caller = resolveUser(principal);
    boolean isAdmin = isAdmin(principal);
    String visibilityStr = req.visibility() != null ? req.visibility().toUpperCase() : "PRIVATE";

    if ("GLOBAL".equals(visibilityStr) && !isAdmin) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(java.util.Map.of("error", "only_admins_can_create_global_questions"));
    }

    Question question;
    if ("GLOBAL".equals(visibilityStr)) {
      question = Question.createGlobal(req.unitId(), req.text().trim(), req.explanation(), req.difficulty());
    } else {
      question = Question.createPrivate(req.unitId(), req.text().trim(), req.explanation(),
          req.difficulty(), caller.getId());
    }

    try {
      Question saved = contentService.createQuestion(question);
      for (AnswerRequest a : req.answers()) {
        answerRepo.save(Answer.create(saved.getId(), a.text().trim(), a.correct()));
      }
      List<Answer> answers = answerRepo.findByQuestionId(saved.getId());
      boolean isEditable = isAdmin || saved.getVisibility() == Visibility.PRIVATE;
      return ResponseEntity.ok(new ManageQuestionResponse(saved.getId(), saved.getUnitId(),
          saved.getText(), saved.getExplanation(), saved.getDifficulty().name(),
          answers.stream().map(AnswerDto::from).toList(), saved.getVisibility(), isEditable));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
    }
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> update(@PathVariable UUID id,
                                   @Valid @RequestBody QuestionRequest req,
                                   @AuthenticationPrincipal User principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    if (req.unitId() == null) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "unit_required"));
    }
    if (req.text() == null || req.text().trim().isEmpty()) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "text_required"));
    }
    if (req.answers() == null || req.answers().size() != 4) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "answers_required"));
    }

    AppUser caller = resolveUser(principal);
    boolean isAdmin = isAdmin(principal);

    Question current = contentService.getQuestionsByUnit(req.unitId()).stream()
        .filter(q -> q.getId().equals(id))
        .findFirst().orElse(null);
    if (current == null) {
      return ResponseEntity.notFound().build();
    }

    boolean canEdit = isAdmin || (current.getVisibility() == Visibility.PRIVATE
        && caller.getId().equals(current.getOwnerId()));
    if (!canEdit) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(java.util.Map.of("error", "not_authorized"));
    }

    Question updated = new Question(id, req.unitId(), req.text().trim(), req.explanation(),
        req.difficulty(), current.getVisibility(), current.getOwnerId());
    try {
      Question saved = contentService.createQuestion(updated);
      answerRepo.deleteByQuestionId(id);
      for (AnswerRequest a : req.answers()) {
        answerRepo.save(Answer.create(saved.getId(), a.text().trim(), a.correct()));
      }
      List<Answer> answers = answerRepo.findByQuestionId(saved.getId());
      boolean isEditable = isAdmin || (saved.getVisibility() == Visibility.PRIVATE
          && caller.getId().equals(saved.getOwnerId()));
      return ResponseEntity.ok(new ManageQuestionResponse(saved.getId(), saved.getUnitId(),
          saved.getText(), saved.getExplanation(), saved.getDifficulty().name(),
          answers.stream().map(AnswerDto::from).toList(), saved.getVisibility(), isEditable));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
    }
  }

  @GetMapping("/export")
  public ResponseEntity<?> export(@RequestParam(required = false) UUID unitId,
                                   @RequestParam(defaultValue = "json") String format,
                                   @AuthenticationPrincipal User principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    AppUser caller = resolveUser(principal);
    boolean isAdmin = isAdmin(principal);
    List<Question> data = unitId == null
        ? (isAdmin ? contentService.getAllQuestions() : contentService.getVisibleQuestions(caller.getId()))
        : contentService.getVisibleQuestionsByUnit(unitId, caller.getId());
    if (format.equalsIgnoreCase("csv")) {
      String csv = toCsv(data);
      return ResponseEntity.ok()
          .header("Content-Type", "text/csv")
          .header("Content-Disposition", "attachment; filename=questions.csv")
          .body(csv);
    }
    List<ManageQuestionResponse> payload = data.stream()
        .map(q -> {
          List<Answer> answers = answerRepo.findByQuestionId(q.getId());
          return new ManageQuestionResponse(q.getId(), q.getUnitId(), q.getText(),
              q.getExplanation(), q.getDifficulty().name(),
              answers.stream().map(AnswerDto::from).toList(),
              q.getVisibility(), true);
        })
        .toList();
    return ResponseEntity.ok(payload);
  }

  private static final long MAX_UPLOAD_SIZE = 10L * 1024 * 1024;
  private static final java.util.Set<String> ALLOWED_CONTENT_TYPES = java.util.Set.of(
      "application/json", "text/csv", "application/octet-stream", "text/plain"
  );

  @PostMapping(value = "/import", consumes = "multipart/form-data")
  public ResponseEntity<?> importQuestions(
      @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
      @RequestParam(defaultValue = "json") String format,
      @RequestParam(required = false) UUID unitId,
      @AuthenticationPrincipal User principal) throws Exception {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    AppUser caller = resolveUser(principal);
    boolean isAdmin = isAdmin(principal);

    if (file.isEmpty()) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "file_required"));
    }
    if (file.getSize() > MAX_UPLOAD_SIZE) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "file_too_large"));
    }
    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.split(";")[0].trim())) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "invalid_content_type"));
    }

    int created = 0;
    int errors = 0;
    Visibility importVisibility = isAdmin ? Visibility.GLOBAL : Visibility.PRIVATE;
    UUID importOwner = isAdmin ? null : caller.getId();

    if (format.equalsIgnoreCase("csv")) {
      String content = new String(file.getBytes());
      List<String[]> rows = parseCsv(content);
      for (int i = 1; i < rows.size(); i++) {
        try {
          ImportRow row = csvToImportRow(rows.get(i), unitId);
          if (row == null) { errors++; continue; }
          saveImportedQuestion(row, importVisibility, importOwner);
          created++;
        } catch (Exception e) {
          errors++;
        }
      }
    } else {
      com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
      ImportRow[] rows = mapper.readValue(file.getBytes(), ImportRow[].class);
      for (ImportRow row : rows) {
        ImportRow effective = unitId != null ? new ImportRow(unitId, row.text(), row.explanation(),
            row.difficulty(), row.answers()) : row;
        if (effective.unitId() == null || effective.text() == null) { errors++; continue; }
        try {
          saveImportedQuestion(effective, importVisibility, importOwner);
          created++;
        } catch (Exception e) {
          errors++;
        }
      }
    }
    return ResponseEntity.ok(java.util.Map.of("created", created, "errors", errors));
  }

  private void saveImportedQuestion(ImportRow row, Visibility visibility, UUID ownerId) {
    Question question = visibility == Visibility.GLOBAL
        ? Question.createGlobal(row.unitId(), row.text().trim(), row.explanation(), row.difficulty())
        : Question.createPrivate(row.unitId(), row.text().trim(), row.explanation(), row.difficulty(), ownerId);
    Question saved = contentService.createQuestion(question);
    for (AnswerRequest a : row.answers()) {
      answerRepo.save(Answer.create(saved.getId(), a.text().trim(), a.correct()));
    }
  }

  private String toCsv(List<Question> data) {
    StringBuilder sb = new StringBuilder();
    sb.append("unitId,text,explanation,difficulty,answer1,answer2,answer3,answer4,correctIndex\n");
    for (Question q : data) {
      List<Answer> list = answerRepo.findByQuestionId(q.getId());
      if (list.size() != 4) continue;
      int correct = 1;
      for (int i = 0; i < list.size(); i++) { if (list.get(i).isCorrect()) correct = i + 1; }
      sb.append(escape(q.getUnitId().toString())).append(',')
          .append(escape(q.getText())).append(',')
          .append(escape(q.getExplanation() == null ? "" : q.getExplanation())).append(',')
          .append(q.getDifficulty()).append(',')
          .append(escape(list.get(0).getText())).append(',')
          .append(escape(list.get(1).getText())).append(',')
          .append(escape(list.get(2).getText())).append(',')
          .append(escape(list.get(3).getText())).append(',')
          .append(correct).append('\n');
    }
    return sb.toString();
  }

  private String escape(String value) {
    String v = value == null ? "" : value;
    if (!v.isEmpty() && (v.charAt(0) == '=' || v.charAt(0) == '+' || v.charAt(0) == '-' || v.charAt(0) == '@')) {
      v = "'" + v;
    }
    if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
      return "\"" + v.replace("\"", "\"\"") + "\"";
    }
    return v;
  }

  private List<String[]> parseCsv(String content) {
    List<String[]> rows = new java.util.ArrayList<>();
    StringBuilder current = new StringBuilder();
    List<String> row = new java.util.ArrayList<>();
    boolean inQuotes = false;
    for (int i = 0; i < content.length(); i++) {
      char c = content.charAt(i);
      if (c == '"') {
        if (inQuotes && i + 1 < content.length() && content.charAt(i + 1) == '"') {
          current.append('"'); i++;
        } else { inQuotes = !inQuotes; }
      } else if (c == ',' && !inQuotes) {
        row.add(current.toString()); current.setLength(0);
      } else if ((c == '\n' || c == '\r') && !inQuotes) {
        if (current.length() > 0 || !row.isEmpty()) {
          row.add(current.toString());
          rows.add(row.toArray(new String[0]));
          row = new java.util.ArrayList<>();
          current.setLength(0);
        }
      } else { current.append(c); }
    }
    if (current.length() > 0 || !row.isEmpty()) {
      row.add(current.toString()); rows.add(row.toArray(new String[0]));
    }
    return rows;
  }

  private ImportRow csvToImportRow(String[] row, UUID overrideUnitId) {
    UUID unitId;
    int col;
    if (overrideUnitId != null) {
      unitId = overrideUnitId;
      col = (row.length >= 9) ? 1 : 0;
      if (row.length < col + 8) return null;
    } else {
      if (row.length < 9) return null;
      unitId = UUID.fromString(row[0].trim());
      col = 1;
    }
    String text = row[col];
    String explanation = row[col + 1].isBlank() ? null : row[col + 1];
    Question.Difficulty difficulty = Question.Difficulty.valueOf(row[col + 2].trim());
    List<AnswerRequest> answers = new java.util.ArrayList<>(List.of(
        new AnswerRequest(row[col + 3], false),
        new AnswerRequest(row[col + 4], false),
        new AnswerRequest(row[col + 5], false),
        new AnswerRequest(row[col + 6], false)
    ));
    int correct = Integer.parseInt(row[col + 7].trim());
    AnswerRequest picked = answers.get(Math.max(0, Math.min(3, correct - 1)));
    answers.set(Math.max(0, Math.min(3, correct - 1)), new AnswerRequest(picked.text(), true));
    return new ImportRow(unitId, text, explanation, difficulty, answers);
  }

  record ImportRow(UUID unitId, String text, String explanation,
                   Question.Difficulty difficulty, List<AnswerRequest> answers) {}

  @DeleteMapping("/{id}")
  public ResponseEntity<?> delete(@PathVariable UUID id,
                                   @AuthenticationPrincipal User principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    AppUser caller = resolveUser(principal);
    boolean isAdmin = isAdmin(principal);
    try {
      contentService.deleteQuestionIfAuthorized(id, caller.getId(), isAdmin);
      return ResponseEntity.ok().build();
    } catch (AccessDeniedException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(java.util.Map.of("error", e.getMessage()));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  private AppUser resolveUser(User principal) {
    return userRepo.findByEmail(principal.getUsername())
        .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
            HttpStatus.UNAUTHORIZED));
  }

  private boolean isAdmin(User principal) {
    return principal.getAuthorities().stream()
        .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
  }

  public record AnswerRequest(String text, boolean correct) {}

  public record QuestionRequest(UUID unitId, String text, String explanation,
                                 Question.Difficulty difficulty, List<AnswerRequest> answers,
                                 String visibility) {}

  public record AnswerDto(UUID id, String text, boolean correct) {
    static AnswerDto from(Answer a) {
      return new AnswerDto(a.getId(), a.getText(), a.isCorrect());
    }
  }

  public record ManageQuestionResponse(UUID id, UUID unitId, String text, String explanation,
                                        String difficulty, List<AnswerDto> answers,
                                        Visibility visibility, boolean isEditable) {}

  record PageResponse<T>(List<T> items, int page, int size, long total, int totalPages) {
    static <T, R> PageResponse<R> from(Page<T> data, Function<T, R> mapper) {
      return new PageResponse<>(
          data.getContent().stream().map(mapper).toList(),
          data.getNumber(),
          data.getSize(),
          data.getTotalElements(),
          data.getTotalPages()
      );
    }
  }
}
