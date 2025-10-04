package com.course.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "books", schema = "courses")
@JsonIgnoreProperties(ignoreUnknown = true)                // garde l’ordre
public class Book {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String authors;
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Chapter> chapters = new ArrayList<>();

    protected Book() {/* JPA */}
    public Book(String title, String authors) { this.title = title; this.authors = authors; }

    public void addChapter(Chapter c) { c.setBook(this); chapters.add(c); }
    public void setCreatedAt(LocalDateTime at) { this.createdAt = at; }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthors() {
        return authors;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<Chapter> getChapters() {
        return chapters;
    }
}