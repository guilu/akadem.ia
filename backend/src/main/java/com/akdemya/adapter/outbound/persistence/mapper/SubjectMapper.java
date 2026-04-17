package com.akdemya.adapter.outbound.persistence.mapper;

import com.akdemya.adapter.outbound.persistence.entity.SubjectEntity;
import com.akdemya.adapter.outbound.persistence.entity.SyllabusEntity;
import com.akdemya.adapter.outbound.persistence.repository.SpringDataSyllabusRepository;
import com.akdemya.domain.model.Subject;
import com.akdemya.domain.model.Visibility;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SubjectMapper {

  private final SpringDataSyllabusRepository syllabusRepository;

  public SubjectMapper(SpringDataSyllabusRepository syllabusRepository) {
    this.syllabusRepository = syllabusRepository;
  }

  public Subject toDomain(SubjectEntity entity) {
    Visibility visibility = entity.getVisibility() != null
        ? Visibility.valueOf(entity.getVisibility())
        : Visibility.GLOBAL;
    UUID syllabusId = entity.getSyllabus() != null ? entity.getSyllabus().getId() : null;
    return new Subject(entity.getId(), entity.getName(), entity.getDescription(),
        visibility, entity.getOwnerId(), syllabusId);
  }

  public SubjectEntity toEntity(Subject domain) {
    SubjectEntity entity = new SubjectEntity(domain.getName(), domain.getDescription());
    entity.setId(domain.getId());
    entity.setVisibility(domain.getVisibility() != null ? domain.getVisibility().name() : Visibility.GLOBAL.name());
    entity.setOwnerId(domain.getOwnerId());
    if (domain.getSyllabusId() != null) {
      SyllabusEntity syllabusEntity = syllabusRepository.findById(domain.getSyllabusId())
          .orElseThrow(() -> new IllegalArgumentException("Syllabus not found: " + domain.getSyllabusId()));
      entity.setSyllabus(syllabusEntity);
    }
    return entity;
  }
}
