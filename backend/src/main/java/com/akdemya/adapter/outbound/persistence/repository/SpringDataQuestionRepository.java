package com.akdemya.adapter.outbound.persistence.repository;

import com.akdemya.adapter.outbound.persistence.entity.QuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SpringDataQuestionRepository extends JpaRepository<QuestionEntity, UUID> {
    List<QuestionEntity> findByUnit_Id(UUID unitId);
    org.springframework.data.domain.Page<QuestionEntity> findByUnit_Id(UUID unitId, org.springframework.data.domain.Pageable pageable);
    long countByUnit_Id(UUID unitId);
}
