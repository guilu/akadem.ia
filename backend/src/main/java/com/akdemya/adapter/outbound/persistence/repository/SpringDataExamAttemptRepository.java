package com.akdemya.adapter.outbound.persistence.repository;

import com.akdemya.adapter.outbound.persistence.entity.ExamAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SpringDataExamAttemptRepository extends JpaRepository<ExamAttemptEntity, UUID> {
  List<ExamAttemptEntity> findByUserEmail(String email);

  List<ExamAttemptEntity> findByUserEmailOrderByStartedAtDesc(String email);
}
