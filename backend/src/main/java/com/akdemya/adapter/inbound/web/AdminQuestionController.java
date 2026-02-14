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

  @PostMapping
  public ResponseEntity<?> create(@RequestBody QuestionRequest req) {
    var validation = validateRequest(req);
    if (validation != null) return validation;

    Question question = Question.create(req.unitId(), req.text().trim(), req.explanation(), req.difficulty());
    Question saved = questions.save(question);
    for (AnswerRequest a : req.answers()) {
      answers.save(Answer.create(saved.getId(), a.text().trim(), a.correct()));
    }
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
