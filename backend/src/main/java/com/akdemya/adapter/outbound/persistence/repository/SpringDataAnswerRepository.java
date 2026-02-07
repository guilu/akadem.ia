package com.akdemya.adapter.outbound.persistence.repository;

import com.akdemya.adapter.outbound.persistence.entity.AnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SpringDataAnswerRepository extends JpaRepository<AnswerEntity, UUID> {
    List<AnswerEntity> findByQuestion_Id(UUID questionId);
}
