package com.course.dto;
import java.util.List;

public record QuizDTO(
        Long id,
        String label,
        List<QuizQuestionDTO> questions
) {}
