
package com.user.controller;

import com.user.model.Progress;
import com.user.repository.ProgressRepository;
import com.user.service.CourseClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final ProgressRepository progressRepo;
    private final CourseClient courseClient;

    public ProgressController(ProgressRepository progressRepo, CourseClient courseClient) {
        this.progressRepo = progressRepo;
        this.courseClient = courseClient;
    }

    @GetMapping("/{userId}")
    public List<Progress> getProgress(@PathVariable Long userId,
                                      @RequestParam(required = false) Long bookId) {
        return (bookId == null)
                ? progressRepo.findByUserId(userId)
                : progressRepo.findByUserIdAndBookId(userId, bookId);
    }

    @PostMapping
    public Progress create(@RequestBody Progress progress) {
        return progressRepo.save(progress);
    }

    /** Webhook simple : accepte un 'status' optionnel et complète endTimestamp si completed */
    @PostMapping("/webhook")
    public Progress webhook(@RequestBody Map<String, Object> payload) {
        Progress p = new Progress();
        p.setUserId(((Number) payload.get("userId")).longValue());
        p.setBookId(((Number) payload.get("bookId")).longValue());
        if (payload.get("chapterId") != null) p.setChapterId(((Number) payload.get("chapterId")).longValue());
        if (payload.get("subchapterId") != null) p.setSubchapterId(((Number) payload.get("subchapterId")).longValue());
        if (payload.get("sectionId") != null) p.setSectionId(((Number) payload.get("sectionId")).longValue());
        if (payload.get("sectionRating") != null) p.setSectionRating(((Number) payload.get("sectionRating")).doubleValue());
        if (payload.get("sectionTime") != null) p.setSectionTime(((Number) payload.get("sectionTime")).longValue());
        if (payload.get("startTimestamp") != null) p.setStartTimestamp(OffsetDateTime.parse((String) payload.get("startTimestamp")));
        if (payload.get("endTimestamp") != null) p.setEndTimestamp(OffsetDateTime.parse((String) payload.get("endTimestamp")));
        String status = (String) payload.getOrDefault("status", "");
        if ("completed".equalsIgnoreCase(status) && p.getEndTimestamp() == null) {
            p.setEndTimestamp(OffsetDateTime.now());
        }
        return progressRepo.save(p);
    }

    @GetMapping("/{userId}/summary")
    public ResponseEntity<Map<String, Object>> summary(@PathVariable Long userId,
                                                       @RequestParam(required = false) Long bookId) {
        List<Progress> list = (bookId == null)
                ? progressRepo.findByUserId(userId)
                : progressRepo.findByUserIdAndBookId(userId, bookId);
        long count = list.stream().map(Progress::getSectionId).filter(Objects::nonNull).count();
        Long totalTime = progressRepo.sumTime(userId, bookId);
        Map<String, Object> res = new HashMap<>();
        res.put("userId", userId);
        res.put("bookId", bookId);
        res.put("entries", list.size());
        res.put("sectionsCompleted", count);
        res.put("totalTimeSeconds", totalTime == null ? 0 : totalTime);
        return ResponseEntity.ok(res);
    }

    /** Résumé croisé avec course-service : total sections + pourcentage estimé */
    @GetMapping("/{userId}/course/{bookId}/summary")
    public ResponseEntity<Map<String, Object>> courseSummary(@PathVariable Long userId,
                                                             @PathVariable Long bookId) {
        List<Progress> list = progressRepo.findByUserIdAndBookId(userId, bookId);
        Set<Long> completed = list.stream()
                .filter(p -> p.getSectionId() != null)
                .filter(p -> (p.getEndTimestamp() != null) ||
                             (p.getSectionTime() != null && p.getSectionTime() > 0) ||
                             (p.getSectionRating() != null))
                .map(Progress::getSectionId)
                .collect(Collectors.toSet());

        int totalSections = courseClient.countSectionsForBook(bookId);
        double percent = totalSections > 0 ? (completed.size() * 100.0) / totalSections : 0.0;

        Long totalTime = progressRepo.sumTime(userId, bookId);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("userId", userId);
        res.put("bookId", bookId);
        res.put("totalSections", totalSections);
        res.put("completedSections", completed.size());
        res.put("progressPercent", Math.round(percent * 10.0) / 10.0);
        res.put("totalTimeSeconds", totalTime == null ? 0 : totalTime);
        return ResponseEntity.ok(res);
    }
}
