package com.akdemya.adapter.inbound.web;

import com.akdemya.adapter.inbound.web.dto.QuestionDTO;
import com.akdemya.application.service.ContentManagement;
import com.akdemya.domain.model.Answer;
import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.model.Question;
import com.akdemya.domain.model.Visibility;
import com.akdemya.domain.port.out.UserRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final ContentManagement contentService;
    private final UserRepository userRepo;

    public QuestionController(ContentManagement contentService, UserRepository userRepo) {
        this.contentService = contentService;
        this.userRepo = userRepo;
    }

    @GetMapping
    public List<QuestionDTO> getByUnit(@RequestParam UUID unitId,
                                       @AuthenticationPrincipal User principal) {
        List<Question> questions;
        if (principal == null) {
            // Unauthenticated — return only global questions
            questions = contentService.getQuestionsByUnit(unitId);
        } else {
            UUID userId = resolveUserId(principal);
            questions = contentService.getVisibleQuestionsByUnit(unitId, userId);
        }
        return questions.stream().map(q -> {
            var answers = contentService.getAnswersByQuestion(q.getId()).stream()
                    .map(a -> new QuestionDTO.AnswerDTO(a.getId(), a.getText()))
                    .collect(Collectors.toList());
            return new QuestionDTO(q.getId(), q.getText(), answers);
        }).collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<Question> create(@Valid @RequestBody CreateQuestionRequest req,
                                           @AuthenticationPrincipal User principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID userId = resolveUserId(principal);
        Visibility visibility = req.visibility() != null ? req.visibility() : Visibility.PRIVATE;

        // Only ADMINs can create GLOBAL questions
        if (visibility == Visibility.GLOBAL && !isAdmin(principal)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Question question;
        if (visibility == Visibility.GLOBAL) {
            question = Question.createGlobal(req.unitId(), req.text(), req.explanation(), req.difficulty());
        } else {
            question = Question.createPrivate(req.unitId(), req.text(), req.explanation(), req.difficulty(), userId);
        }
        return ResponseEntity.ok(contentService.createQuestion(question));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id,
                                    @AuthenticationPrincipal User principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID userId = resolveUserId(principal);
        boolean admin = isAdmin(principal);
        try {
            contentService.deleteQuestionIfAuthorized(id, userId, admin);
            contentService.deleteAnswersByQuestion(id);
            return ResponseEntity.ok().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(java.util.Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{qid}/answers")
    public Answer addAnswer(@PathVariable UUID qid, @Valid @RequestBody Answer a) {
        Answer toSave = new Answer(a.getId(), qid, a.getText(), a.isCorrect());
        return contentService.createAnswer(toSave);
    }

    private UUID resolveUserId(User principal) {
        return userRepo.findByEmail(principal.getUsername())
                .map(AppUser::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private boolean isAdmin(User principal) {
        return principal.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    public record CreateQuestionRequest(UUID unitId, String text, String explanation,
                                        Question.Difficulty difficulty, Visibility visibility) {}
}
