package com.akdemya.adapter.inbound.web;

import com.akdemya.domain.port.in.ExamUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/exams")
@CrossOrigin(origins = "*")
public class ExamController {

    private final ExamUseCase examUseCase;

    public ExamController(ExamUseCase examUseCase) {
        this.examUseCase = examUseCase;
    }

    public record StartRequest(java.util.Map<UUID, Integer> unitCounts, int minutes) {
    }

    @PostMapping("/attempts/start")
    public ResponseEntity<?> start(@RequestBody StartRequest req, @AuthenticationPrincipal User principal) {
        String email = principal != null ? principal.getUsername() : "guest@akdemya";
        // Construct Command with email
        ExamUseCase.StartCommand command = new ExamUseCase.StartCommand(email, req.unitCounts(), req.minutes());
        var response = examUseCase.startExam(command);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public ResponseEntity<?> submit(@PathVariable UUID attemptId, @RequestBody ExamUseCase.SubmitCommand req,
            @AuthenticationPrincipal User principal) {
        if (req.attemptId() != null && !req.attemptId().equals(attemptId)) {
            // ignore mismatch and use path param
        }
        ExamUseCase.SubmitCommand command = new ExamUseCase.SubmitCommand(attemptId, req.selections());
        var result = examUseCase.submitExam(command);
        return ResponseEntity.ok(result);
    }

    public record UpdateAnswerRequest(UUID selectedAnswerId) {
    }

    @PutMapping("/attempts/{attemptId}/answers/{questionId}")
    public ResponseEntity<?> updateAnswer(@PathVariable UUID attemptId, @PathVariable UUID questionId,
            @RequestBody UpdateAnswerRequest req) {
        if (req == null || req.selectedAnswerId() == null) {
            return ResponseEntity.badRequest().body("selectedAnswerId requerido");
        }
        examUseCase.updateAnswer(new ExamUseCase.UpdateAnswerCommand(attemptId, questionId, req.selectedAnswerId()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/attempts/{attemptId}")
    public ResponseEntity<?> getAttempt(@PathVariable UUID attemptId) {
        var result = examUseCase.getAttempt(attemptId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/attempts")
    public ResponseEntity<?> listAttempts(@AuthenticationPrincipal User principal) {
        String email = principal != null ? principal.getUsername() : "guest@akdemya";
        var result = examUseCase.listAttempts(email);
        return ResponseEntity.ok(result);
    }
}
