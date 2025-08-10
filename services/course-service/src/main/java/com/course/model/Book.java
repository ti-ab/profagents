
package com.course.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "books", schema = "courses")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column
    private String authors;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Chapter> chapters = new ArrayList<>();

    
    /* ===== Constructors ===== */
    public Book() { }

    public Book(String title, String authors) {
        this.title = title;
        this.authors = authors;
    }
/* ===== Getters / Setters ===== */
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthors() { return authors; }
    public void setAuthors(String authors) { this.authors = authors; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<Chapter> getChapters() { return chapters; }
    public void setChapters(List<Chapter> chapters) {
        this.chapters = chapters != null ? chapters : new ArrayList<>();
        for (Chapter c : this.chapters) {
            c.setBook(this);
        }
    }

    public void addChapter(Chapter chapter) {
        if (chapter == null) return;
        chapters.add(chapter);
        chapter.setBook(this);
    }
}
