package com.akdemya.adapter.outbound.persistence.repository;

import com.akdemya.adapter.outbound.persistence.entity.SubjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SpringDataSubjectRepository extends JpaRepository<SubjectEntity, UUID> {

  @Query("SELECT s FROM SubjectEntity s WHERE s.visibility = 'GLOBAL' OR s.ownerId = :userId")
  List<SubjectEntity> findVisibleByUserId(@Param("userId") UUID userId);
}
