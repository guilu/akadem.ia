package com.akdemya.adapter.outbound.persistence.repository;

import com.akdemya.adapter.outbound.persistence.entity.SyllabusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SpringDataSyllabusRepository extends JpaRepository<SyllabusEntity, UUID> {

  List<SyllabusEntity> findByVisibility(String visibility);

  @Query("SELECT s FROM SyllabusEntity s WHERE s.visibility = 'GLOBAL' OR s.ownerId = :ownerId")
  List<SyllabusEntity> findByVisibilityOrOwnerId(@Param("ownerId") UUID ownerId);
}
