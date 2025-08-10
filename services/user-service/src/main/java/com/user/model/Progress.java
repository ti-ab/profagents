
package com.user.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "progress", schema = "users")
public class Progress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "chapter_id")
    private Long chapterId;

    @Column(name = "subchapter_id")
    private Long subchapterId;

    @Column(name = "section_id")
    private Long sectionId;

    @Column(name = "section_rating")
    private Double sectionRating;

    @Column(name = "section_time")
    private Long sectionTime; // seconds

    @Column(name = "start_timestamp")
    private OffsetDateTime startTimestamp;

    @Column(name = "end_timestamp")
    private OffsetDateTime endTimestamp;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }

    public Long getChapterId() { return chapterId; }
    public void setChapterId(Long chapterId) { this.chapterId = chapterId; }

    public Long getSubchapterId() { return subchapterId; }
    public void setSubchapterId(Long subchapterId) { this.subchapterId = subchapterId; }

    public Long getSectionId() { return sectionId; }
    public void setSectionId(Long sectionId) { this.sectionId = sectionId; }

    public Double getSectionRating() { return sectionRating; }
    public void setSectionRating(Double sectionRating) { this.sectionRating = sectionRating; }

    public Long getSectionTime() { return sectionTime; }
    public void setSectionTime(Long sectionTime) { this.sectionTime = sectionTime; }

    public OffsetDateTime getStartTimestamp() { return startTimestamp; }
    public void setStartTimestamp(OffsetDateTime startTimestamp) { this.startTimestamp = startTimestamp; }

    public OffsetDateTime getEndTimestamp() { return endTimestamp; }
    public void setEndTimestamp(OffsetDateTime endTimestamp) { this.endTimestamp = endTimestamp; }
}
