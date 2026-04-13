package com.akdemya.adapter.outbound.persistence.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "syllabuses")
public class SyllabusEntity {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String visibility = "GLOBAL";

    @Column(name = "owner_id")
    private UUID ownerId;

    @OneToMany(mappedBy = "syllabus")
    private List<SubjectEntity> subjects = new ArrayList<>();

    public SyllabusEntity() {
    }

    public SyllabusEntity(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public List<SubjectEntity> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<SubjectEntity> subjects) {
        this.subjects = subjects;
    }
}
