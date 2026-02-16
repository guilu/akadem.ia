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
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        if (req.attemptId() != null && !req.attemptId().equals(attemptId)) {
            // ignore mismatch and use path param
        }
        try {
            ExamUseCase.SubmitCommand command = new ExamUseCase.SubmitCommand(attemptId, req.selections());
            var result = examUseCase.submitExam(command, principal.getUsername());
            return ResponseEntity.ok(result);
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).build();
        }
    }

    public record UpdateAnswerRequest(UUID selectedAnswerId) {
    }

    @PutMapping("/attempts/{attemptId}/answers/{questionId}")
    public ResponseEntity<?> updateAnswer(@PathVariable UUID attemptId, @PathVariable UUID questionId,
            @RequestBody UpdateAnswerRequest req, @AuthenticationPrincipal User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        if (req == null || req.selectedAnswerId() == null) {
            return ResponseEntity.badRequest().body("selectedAnswerId requerido");
        }
        try {
            examUseCase.updateAnswer(new ExamUseCase.UpdateAnswerCommand(attemptId, questionId, req.selectedAnswerId()),
                    principal.getUsername());
            return ResponseEntity.noContent().build();
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).build();
        }
    }

    @GetMapping("/attempts/{attemptId}")
    public ResponseEntity<?> getAttempt(@PathVariable UUID attemptId, @AuthenticationPrincipal User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            var result = examUseCase.getAttempt(attemptId, principal.getUsername());
            return ResponseEntity.ok(result);
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).build();
        }
    }

    @GetMapping("/attempts")
    public ResponseEntity<?> listAttempts(@AuthenticationPrincipal User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        var result = examUseCase.listAttempts(principal.getUsername());
        return ResponseEntity.ok(result);
    }
}
