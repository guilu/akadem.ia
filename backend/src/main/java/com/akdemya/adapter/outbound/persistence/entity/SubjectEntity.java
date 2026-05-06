package com.akdemya.adapter.outbound.persistence.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "subjects")
public class SubjectEntity {
    @Id
    private UUID id = UUID.randomUUID();
    @Column(nullable = false)
    private String name;
    private String description;

    @Column(nullable = false)
    private String visibility = "GLOBAL";

    @Column(name = "owner_id")
    private UUID ownerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllabus_id", nullable = false)
    private SyllabusEntity syllabus;

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<UnitEntity> units = new java.util.ArrayList<>();

    public SubjectEntity() {
    }

    public SubjectEntity(String name, String description) {
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

    public String getDescription() {
        return description;
    }

    public void setName(String name) {
        this.name = name;
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

    public SyllabusEntity getSyllabus() {
        return syllabus;
    }

    public void setSyllabus(SyllabusEntity syllabus) {
        this.syllabus = syllabus;
    }

    public java.util.List<UnitEntity> getUnits() {
        return units;
    }

    public void setUnits(java.util.List<UnitEntity> units) {
        this.units = units;
    }
}
