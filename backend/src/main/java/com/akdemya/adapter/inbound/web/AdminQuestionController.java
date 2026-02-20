package com.akdemya.adapter.inbound.web;

import com.akdemya.domain.model.Answer;
import com.akdemya.domain.model.Question;
import com.akdemya.domain.port.out.AnswerRepository;
import com.akdemya.domain.port.out.QuestionRepository;
import com.akdemya.domain.port.out.UnitRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/questions")
@CrossOrigin(origins = "*")
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
  public List<QuestionResponse> list(@RequestParam UUID unitId) {
    return questions.findByUnitId(unitId).stream()
        .map(q -> QuestionResponse.from(q, answers.findByQuestionId(q.getId())))
        .toList();
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

  @PostMapping(value = "/import", consumes = "multipart/form-data")
  public ResponseEntity<?> importQuestions(@RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                                           @RequestParam(defaultValue = "json") String format) throws Exception {
    if (file.isEmpty()) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "file_required"));
    }
    int created = 0;
    int errors = 0;
    if (format.equalsIgnoreCase("csv")) {
      String content = new String(file.getBytes());
      List<String[]> rows = parseCsv(content);
      for (int i = 1; i < rows.size(); i++) {
        try {
          QuestionRequest req = csvToRequest(rows.get(i));
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
        var validation = validateRequest(req);
        if (validation != null) {
          errors++;
          continue;
        }
        saveQuestion(req);
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

  private QuestionRequest csvToRequest(String[] row) {
    if (row.length < 9) {
      throw new IllegalArgumentException("invalid_row");
    }
    UUID unitId = UUID.fromString(row[0].trim());
    String text = row[1];
    String explanation = row[2].isBlank() ? null : row[2];
    Question.Difficulty difficulty = Question.Difficulty.valueOf(row[3].trim());
    List<AnswerRequest> list = List.of(
        new AnswerRequest(row[4], false),
        new AnswerRequest(row[5], false),
        new AnswerRequest(row[6], false),
        new AnswerRequest(row[7], false)
    );
    int correct = Integer.parseInt(row[8].trim());
    List<AnswerRequest> answers = new java.util.ArrayList<>(list);
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
}
