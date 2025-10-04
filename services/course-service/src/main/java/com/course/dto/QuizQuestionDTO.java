package com.course.dto;

import java.util.List;

public record QuizQuestionDTO(Integer id, String question, String option1, String option2, String option3, String option4, List<String> answers, String explanation) {}
