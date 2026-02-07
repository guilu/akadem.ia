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
        // Ensure attemptId matches request? Or just pass it.
        // Command has attemptId.
        if (req.attemptId() != null && !req.attemptId().equals(attemptId)) {
            // mismatch? just ignore path var or override
        }
        ExamUseCase.SubmitCommand command = new ExamUseCase.SubmitCommand(attemptId, req.selections());
        var result = examUseCase.submitExam(command);
        return ResponseEntity.ok(result);
    }
}
