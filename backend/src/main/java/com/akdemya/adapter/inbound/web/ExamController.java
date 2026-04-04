package com.akdemya.adapter.inbound.web;

import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.port.in.ExamUseCase;
import com.akdemya.domain.port.out.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private static final Logger log = LoggerFactory.getLogger(ExamController.class);
    private final ExamUseCase examUseCase;
    private final UserRepository userRepo;

    public ExamController(ExamUseCase examUseCase, UserRepository userRepo) {
        this.examUseCase = examUseCase;
        this.userRepo = userRepo;
    }

    public record StartRequest(java.util.Map<UUID, Integer> unitCounts, int minutes, String difficulty) {
    }

    public record StartRandomRequest(UUID subjectId, int count, int minutes, String difficulty) {
    }

    @PostMapping("/attempts/start-random")
    public ResponseEntity<?> startRandom(@RequestBody StartRandomRequest req, @AuthenticationPrincipal User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        UUID userId = resolveUserId(principal);
        var command = new ExamUseCase.StartRandomCommand(
                principal.getUsername(), userId, req.subjectId(), req.count(), req.minutes(), req.difficulty());
        var response = examUseCase.startRandomExam(command);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/attempts/start")
    public ResponseEntity<?> start(@RequestBody StartRequest req, @AuthenticationPrincipal User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        UUID userId = resolveUserId(principal);
        ExamUseCase.StartCommand command = new ExamUseCase.StartCommand(principal.getUsername(), userId, req.unitCounts(), req.minutes(), req.difficulty());
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
            log.warn("Submit attempt not found: {} by {}", attemptId, principal.getUsername());
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            log.warn("Forbidden submit attempt: {} by {}", attemptId, principal.getUsername());
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
            log.warn("Update answer not found: attempt {} question {} by {}", attemptId, questionId,
                    principal.getUsername());
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            log.warn("Forbidden update answer: attempt {} by {}", attemptId, principal.getUsername());
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
            log.warn("Attempt not found: {} by {}", attemptId, principal.getUsername());
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            log.warn("Forbidden get attempt: {} by {}", attemptId, principal.getUsername());
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

    private UUID resolveUserId(User principal) {
        return userRepo.findByEmail(principal.getUsername())
                .map(AppUser::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
