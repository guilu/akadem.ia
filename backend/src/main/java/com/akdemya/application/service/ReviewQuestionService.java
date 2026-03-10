package com.akdemya.application.service;

import com.akdemya.domain.model.Answer;
import com.akdemya.domain.model.GeneratedQuestionDraft;
import com.akdemya.domain.model.Question;
import com.akdemya.domain.port.in.PublishGeneratedQuestionsUseCase;
import com.akdemya.domain.port.in.ReviewGeneratedQuestionsUseCase;
import com.akdemya.domain.port.out.AnswerRepository;
import com.akdemya.domain.port.out.GeneratedQuestionDraftRepository;
import com.akdemya.domain.port.out.QuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class ReviewQuestionService implements ReviewGeneratedQuestionsUseCase, PublishGeneratedQuestionsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReviewQuestionService.class);

    private final GeneratedQuestionDraftRepository draftRepo;
    private final QuestionRepository questionRepo;
    private final AnswerRepository answerRepo;

    public ReviewQuestionService(GeneratedQuestionDraftRepository draftRepo,
                                  QuestionRepository questionRepo,
                                  AnswerRepository answerRepo) {
        this.draftRepo = draftRepo;
        this.questionRepo = questionRepo;
        this.answerRepo = answerRepo;
    }

    @Override
    public List<GeneratedQuestionDraft> listDrafts(UUID sourceId, GeneratedQuestionDraft.Status status) {
        return draftRepo.findBySourceDocumentId(sourceId, status);
    }

    @Override
    @Transactional
    public GeneratedQuestionDraft approve(UUID draftId) {
        GeneratedQuestionDraft draft = draftRepo.findById(draftId)
                .orElseThrow(() -> new NoSuchElementException("Draft not found: " + draftId));

        if (draft.getUnitId() == null) {
            throw new IllegalStateException("Draft has no unitId — cannot publish to question bank: " + draftId);
        }

        publish(draft);
        GeneratedQuestionDraft updated = draftRepo.updateStatus(draftId, GeneratedQuestionDraft.Status.VALIDATED);
        log.info("Draft approved and published: draftId={} unitId={}", draftId, draft.getUnitId());
        return updated;
    }

    @Override
    @Transactional
    public GeneratedQuestionDraft reject(UUID draftId) {
        draftRepo.findById(draftId)
                .orElseThrow(() -> new NoSuchElementException("Draft not found: " + draftId));

        GeneratedQuestionDraft updated = draftRepo.updateStatus(draftId, GeneratedQuestionDraft.Status.REJECTED);
        log.info("Draft rejected: draftId={}", draftId);
        return updated;
    }

    @Override
    @Transactional
    public PublishResult publish(GeneratedQuestionDraft draft) {
        Question.Difficulty difficulty;
        try {
            difficulty = Question.Difficulty.valueOf(draft.getDifficulty());
        } catch (IllegalArgumentException e) {
            difficulty = Question.Difficulty.MEDIUM;
        }

        Question question = Question.create(
                draft.getUnitId(),
                draft.getStatement(),
                draft.getExplanation(),
                difficulty
        );
        question = questionRepo.save(question);

        List<Answer> answers = new ArrayList<>();
        List<String> answerTexts = draft.getAnswers();
        for (int i = 0; i < answerTexts.size(); i++) {
            Answer answer = Answer.create(question.getId(), answerTexts.get(i), i == draft.getCorrectIndex());
            answers.add(answerRepo.save(answer));
        }

        log.info("Published question from draft: questionId={} unitId={}", question.getId(), draft.getUnitId());
        return new PublishResult(question, answers);
    }
}
