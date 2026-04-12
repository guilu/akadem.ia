package com.akdemya.adapter.inbound.web;

import com.akdemya.domain.model.Answer;
import com.akdemya.domain.model.Question;
import com.akdemya.domain.port.out.AnswerRepository;
import com.akdemya.domain.port.out.QuestionRepository;
import com.akdemya.domain.port.out.UnitRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin/questions")
public class AdminQuestionController {
  private final QuestionRepository questions;
  private final AnswerRepository answers;
  private final UnitRepository units;

  public AdminQuestionController(QuestionRepository questions, AnswerRepository answers, UnitRepository units) {
    this.questions = questions;
    this.answers = answers;
    this.units = units;
  }

  @GetMapping
  public PageResponse<QuestionResponse> list(@RequestParam UUID unitId,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size) {
    var data = questions.findPageByUnitId(unitId, page, size);
    return PageResponse.from(data, q -> QuestionResponse.from(q, answers.findByQuestionId(q.getId())));
  }

  @GetMapping("/export")
  public ResponseEntity<?> export(@RequestParam(required = false) UUID unitId,
                                  @RequestParam(defaultValue = "json") String format) {
    List<Question> data = unitId == null ? questions.findAll() : questions.findByUnitId(unitId);
    if (format.equalsIgnoreCase("csv")) {
      String csv = toCsv(data);
      return ResponseEntity.ok()
          .header("Content-Type", "text/csv")
          .header("Content-Disposition", "attachment; filename=questions.csv")
          .body(csv);
    }
    List<QuestionResponse> payload = data.stream()
        .map(q -> QuestionResponse.from(q, answers.findByQuestionId(q.getId())))
        .toList();
    return ResponseEntity.ok(payload);
  }

  private static final long MAX_UPLOAD_SIZE = 10 * 1024 * 1024; // 10 MB
  private static final java.util.Set<String> ALLOWED_CONTENT_TYPES = java.util.Set.of(
      "application/json", "text/csv", "application/octet-stream", "text/plain"
  );

  @PostMapping(value = "/import", consumes = "multipart/form-data")
  public ResponseEntity<?> importQuestions(@RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                                           @RequestParam(defaultValue = "json") String format,
                                           @RequestParam(required = false) UUID unitId) throws Exception {
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
    if (format.equalsIgnoreCase("csv")) {
      String content = new String(file.getBytes());
      List<String[]> rows = parseCsv(content);
      for (int i = 1; i < rows.size(); i++) {
        try {
          QuestionRequest req = csvToRequest(rows.get(i), unitId);
          var validation = validateRequest(req);
          if (validation != null) {
            errors++;
            continue;
          }
          saveQuestion(req);
          created++;
        } catch (Exception e) {
          errors++;
        }
      }
    } else {
      com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
      QuestionRequest[] reqs = mapper.readValue(file.getBytes(), QuestionRequest[].class);
      for (QuestionRequest req : reqs) {
        QuestionRequest effective = unitId != null ? new QuestionRequest(unitId, req.text(), req.explanation(), req.difficulty(), req.answers()) : req;
        var validation = validateRequest(effective);
        if (validation != null) {
          errors++;
          continue;
        }
        saveQuestion(effective);
        created++;
      }
    }
    return ResponseEntity.ok(java.util.Map.of("created", created, "errors", errors));
  }

  @PostMapping
  public ResponseEntity<?> create(@RequestBody QuestionRequest req) {
    var validation = validateRequest(req);
    if (validation != null) return validation;

    Question saved = saveQuestion(req);
    return ResponseEntity.ok(QuestionResponse.from(saved, answers.findByQuestionId(saved.getId())));
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody QuestionRequest req) {
    var validation = validateRequest(req);
    if (validation != null) return validation;

    Question current = questions.findById(id).orElse(null);
    if (current == null) return ResponseEntity.notFound().build();

    Question updated = new Question(id, req.unitId(), req.text().trim(), req.explanation(), req.difficulty());
    Question saved = questions.save(updated);
    answers.deleteByQuestionId(id);
    for (AnswerRequest a : req.answers()) {
      answers.save(Answer.create(saved.getId(), a.text().trim(), a.correct()));
    }
    return ResponseEntity.ok(QuestionResponse.from(saved, answers.findByQuestionId(saved.getId())));
  }

  private Question saveQuestion(QuestionRequest req) {
    Question question = Question.create(req.unitId(), req.text().trim(), req.explanation(), req.difficulty());
    Question saved = questions.save(question);
    for (AnswerRequest a : req.answers()) {
      answers.save(Answer.create(saved.getId(), a.text().trim(), a.correct()));
    }
    return saved;
  }

  private String toCsv(List<Question> data) {
    StringBuilder sb = new StringBuilder();
    sb.append("unitId,text,explanation,difficulty,answer1,answer2,answer3,answer4,correctIndex\n");
    for (Question q : data) {
      List<Answer> list = answers.findByQuestionId(q.getId());
      if (list.size() != 4) continue;
      int correct = 1;
      for (int i = 0; i < list.size(); i++) {
        if (list.get(i).isCorrect()) correct = i + 1;
      }
      sb.append(escape(q.getUnitId().toString())).append(',')
          .append(escape(q.getText())).append(',')
          .append(escape(q.getExplanation() == null ? "" : q.getExplanation())).append(',')
          .append(q.getDifficulty()).append(',')
          .append(escape(list.get(0).getText())).append(',')
          .append(escape(list.get(1).getText())).append(',')
          .append(escape(list.get(2).getText())).append(',')
          .append(escape(list.get(3).getText())).append(',')
          .append(correct)
          .append('\n');
    }
    return sb.toString();
  }

  private String escape(String value) {
    String v = value == null ? "" : value;
    // Prevent CSV injection: prefix formula-triggering characters with a single quote
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
          current.append('"');
          i++;
        } else {
          inQuotes = !inQuotes;
        }
      } else if (c == ',' && !inQuotes) {
        row.add(current.toString());
        current.setLength(0);
      } else if ((c == '\n' || c == '\r') && !inQuotes) {
        if (current.length() > 0 || !row.isEmpty()) {
          row.add(current.toString());
          rows.add(row.toArray(new String[0]));
          row = new java.util.ArrayList<>();
          current.setLength(0);
        }
      } else {
        current.append(c);
      }
    }
    if (current.length() > 0 || !row.isEmpty()) {
      row.add(current.toString());
      rows.add(row.toArray(new String[0]));
    }
    return rows;
  }

  private QuestionRequest csvToRequest(String[] row, UUID overrideUnitId) {
    // When overrideUnitId is provided: supports 8-col (no unitId) or 9-col (unitId col ignored)
    // When overrideUnitId is null: requires 9-col with unitId in first column
    UUID unitId;
    int col;
    if (overrideUnitId != null) {
      unitId = overrideUnitId;
      col = (row.length >= 9) ? 1 : 0;
      if (row.length < col + 8) throw new IllegalArgumentException("invalid_row");
    } else {
      if (row.length < 9) throw new IllegalArgumentException("invalid_row");
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
    return new QuestionRequest(unitId, text, explanation, difficulty, answers);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> delete(@PathVariable UUID id) {
    questions.deleteById(id);
    return ResponseEntity.ok().build();
  }

  private ResponseEntity<?> validateRequest(QuestionRequest req) {
    if (req.unitId() == null) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "unit_required"));
    }
    if (units.findById(req.unitId()).isEmpty()) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "unit_not_found"));
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
    long correct = req.answers().stream().filter(AnswerRequest::correct).count();
    if (correct != 1) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "one_correct_required"));
    }
    if (req.answers().stream().anyMatch(a -> a.text() == null || a.text().trim().isEmpty())) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "answer_text_required"));
    }
    return null;
  }

  record AnswerRequest(String text, boolean correct) {}
  record QuestionRequest(UUID unitId, String text, String explanation, Question.Difficulty difficulty, List<AnswerRequest> answers) {}
  record AnswerResponse(UUID id, String text, boolean correct) {
    static AnswerResponse from(Answer a) {
      return new AnswerResponse(a.getId(), a.getText(), a.isCorrect());
    }
  }
  record QuestionResponse(UUID id, UUID unitId, String text, String explanation, Question.Difficulty difficulty, List<AnswerResponse> answers) {
    static QuestionResponse from(Question q, List<Answer> answers) {
      return new QuestionResponse(q.getId(), q.getUnitId(), q.getText(), q.getExplanation(), q.getDifficulty(),
          answers.stream().map(AnswerResponse::from).toList());
    }
  }

  record PageResponse<T>(java.util.List<T> items, int page, int size, long total, int totalPages) {
    static <T, R> PageResponse<R> from(org.springframework.data.domain.Page<T> data, java.util.function.Function<T, R> mapper) {
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
