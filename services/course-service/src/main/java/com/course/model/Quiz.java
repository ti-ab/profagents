package com.course.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Quiz stocké en table dédiée, associé à une Section (1-1).
 * Les questions sont stockées en table "quiz_question" via @ElementCollection,
 * ce qui évite d'avoir une entité supplémentaire.
 */
@Entity
@Table(name = "quiz", schema = "courses")
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Optionnel : un titre/localisation pour debug/BO */
    private String label;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("questionNo ASC")
    private List<QuizQuestion> questions = new ArrayList<>();

    public void setQuestions(List<QuizQuestion> questions) {
        this.questions = (questions != null) ? questions : new ArrayList<>();
    }

    public Quiz() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<QuizQuestion> getQuestions() {
        return questions;
    }
}
