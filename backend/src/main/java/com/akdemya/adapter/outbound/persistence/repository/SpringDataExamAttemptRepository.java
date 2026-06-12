package com.akdemya.adapter.outbound.persistence.repository;

import com.akdemya.adapter.outbound.persistence.entity.ExamAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface SpringDataExamAttemptRepository extends JpaRepository<ExamAttemptEntity, UUID> {
  List<ExamAttemptEntity> findByUserEmail(String email);

  List<ExamAttemptEntity> findByUserEmailOrderByStartedAtDesc(String email);

  /**
   * Atomically finishes an attempt. The {@code finishedAt IS NULL} filter
   * makes concurrent submits race-safe: only the first one wins (returns 1),
   * later ones see 0 affected rows and must keep the stored result.
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("UPDATE ExamAttemptEntity e SET e.finishedAt = :finishedAt, e.score = :score "
      + "WHERE e.id = :id AND e.finishedAt IS NULL")
  int finishIfUnfinished(@Param("id") UUID id,
                         @Param("finishedAt") OffsetDateTime finishedAt,
                         @Param("score") Integer score);
}
