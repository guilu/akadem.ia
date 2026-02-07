package com.akdemya.adapter.outbound.persistence.repository;

import com.akdemya.adapter.outbound.persistence.entity.UnitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SpringDataUnitRepository extends JpaRepository<UnitEntity, UUID> {
    List<UnitEntity> findBySubject_IdOrderByOrderIndexAsc(UUID subjectId);
}
