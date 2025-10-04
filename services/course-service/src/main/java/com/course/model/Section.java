package com.course.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity @Table(name = "sections", schema = "courses")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Section {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String sectionSummary;
    private String firstParagraph;
    private String middleParagraph;
    private String endParagraph;
    @Column(nullable = false)
    private int idx;
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "subchapter_id")
    private Subchapter subchapter;
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    public Quiz getQuiz() { return quiz; }
    public void setQuiz(Quiz quiz) { this.quiz = quiz; }

    public String title() { return title; }
    public String sectionSummary() { return sectionSummary; }
    public String firstParagraph() { return firstParagraph; }
    public String middleParagraph() { return middleParagraph; }
    public String endParagraph()   { return endParagraph; }
    public void setContent(String c) { this.content = c; }
    public void setSubchapter(Subchapter sc) { this.subchapter = sc; }

    public int getIdx() {
        return idx;
    }

    public void setIdx(int idx) {
        this.idx = idx;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSectionSummary() {
        return sectionSummary;
    }

    public String getFirstParagraph() {
        return firstParagraph;
    }

    public String getMiddleParagraph() {
        return middleParagraph;
    }

    public String getEndParagraph() {
        return endParagraph;
    }

    public String getContent() {
        return content;
    }

    public Subchapter getSubchapter() {
        return subchapter;
    }
}