
package com.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class CourseClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public CourseClient(RestTemplate restTemplate,
                        @Value("${course.service.url:http://course-service:8082}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public int countSectionsForBook(Long bookId) {
        String url = baseUrl + "/api/books/" + bookId;
        ResponseEntity<BookDTO> res = restTemplate.getForEntity(url, BookDTO.class);
        BookDTO dto = res.getBody();
        if (dto == null || dto.chapters() == null) return 0;
        int total = 0;
        for (ChapterDTO ch : dto.chapters()) {
            if (ch.subchapters() == null) continue;
            for (SubchapterDTO sc : ch.subchapters()) {
                if (sc.sections() == null) continue;
                total += sc.sections().size();
            }
        }
        return total;
    }

    // DTOs mirroring the course-service payload shape
    public record BookDTO(Long id, String title, String authors, List<ChapterDTO> chapters) {}
    public record ChapterDTO(Long id, int idx, String title, List<SubchapterDTO> subchapters) {}
    public record SubchapterDTO(Long id, int idx, String title, List<SectionDTO> sections) {}
    public record SectionDTO(Long id, int idx, String title, String content) {}
}
