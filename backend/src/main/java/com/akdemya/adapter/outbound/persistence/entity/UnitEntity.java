package com.akdemya.adapter.outbound.persistence.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "units")
public class UnitEntity {
    @Id
    private UUID id = UUID.randomUUID();
    @Column(nullable = false)
    private String name;
    @Column(columnDefinition = "text")
    private String description;
    @Column(name = "order_index", nullable = false)
    private int orderIndex = 0;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private SubjectEntity subject;

    @OneToMany(mappedBy = "unit", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<QuestionEntity> questions = new java.util.ArrayList<>();

    public UnitEntity() {
    }

    public UnitEntity(SubjectEntity subject, String name, String description, int orderIndex) {
        this.subject = subject;
        this.name = name;
        this.description = description;
        this.orderIndex = orderIndex;
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

    public int getOrderIndex() {
        return orderIndex;
    }

    public UUID getSubjectId() {
        return subject != null ? subject.getId() : null;
    }

    public SubjectEntity getSubject() {
        return subject;
    }

    public java.util.List<QuestionEntity> getQuestions() {
        return questions;
    }

    public void setQuestions(java.util.List<QuestionEntity> questions) {
        this.questions = questions;
    }

    public void setName(String n) {
        this.name = n;
    }

    public void setDescription(String d) {
        this.description = d;
    }

    public void setOrderIndex(int i) {
        this.orderIndex = i;
    }

    public void setSubject(SubjectEntity s) {
        this.subject = s;
    }
}
