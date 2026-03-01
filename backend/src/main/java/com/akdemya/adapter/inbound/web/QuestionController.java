package com.akdemya.adapter.inbound.web;

import com.akdemya.adapter.inbound.web.dto.QuestionDTO;
import com.akdemya.application.service.ContentManagement;
import com.akdemya.domain.model.Answer;
import com.akdemya.domain.model.Question;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final ContentManagement contentService;

    public QuestionController(ContentManagement contentService) {
        this.contentService = contentService;
    }

    @GetMapping
    public List<QuestionDTO> getByUnit(@RequestParam UUID unitId) {
        return contentService.getQuestionsByUnit(unitId).stream().map(q -> {
            var answers = contentService.getAnswersByQuestion(q.getId()).stream()
                    .map(a -> new QuestionDTO.AnswerDTO(a.getId(), a.getText()))
                    .collect(Collectors.toList());
            return new QuestionDTO(q.getId(), q.getText(), answers); // QuestionDTO is simpler than domain model for
                                                                     // listing?
        }).collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Question create(@RequestBody Question q) {
        return contentService.createQuestion(q);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        contentService.deleteAnswersByQuestion(id);
        contentService.deleteQuestion(id);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{qid}/answers")
    public Answer addAnswer(@PathVariable UUID qid, @RequestBody Answer a) {
        // Ensure question match?
        // Domain model Answer has questionId.
        Answer toSave = new Answer(a.getId(), qid, a.getText(), a.isCorrect());
        return contentService.createAnswer(toSave);
    }
}
