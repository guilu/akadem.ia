package com.akdemya.adapter.outbound.persistence.repository;

import com.akdemya.adapter.outbound.persistence.entity.FlashcardEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaFlashcardRepository extends JpaRepository<FlashcardEntity, UUID> {

  List<FlashcardEntity> findByUnitId(UUID unitId);
}
