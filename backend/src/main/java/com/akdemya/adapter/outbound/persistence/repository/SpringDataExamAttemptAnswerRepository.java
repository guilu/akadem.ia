package com.akdemya.adapter.outbound.persistence.repository;

import com.akdemya.adapter.outbound.persistence.entity.ExamAttemptAnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SpringDataExamAttemptAnswerRepository extends JpaRepository<ExamAttemptAnswerEntity, UUID> {
    List<ExamAttemptAnswerEntity> findByAttemptId(UUID attemptId);

    java.util.Optional<ExamAttemptAnswerEntity> findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId);
}
