package com.course.model;

import com.course.model.Quiz;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_question", schema = "courses")
public class QuizQuestion {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                  // PK DB

    @Column(name = "question_no", nullable = false)
    private Integer questionNo;       // ton ancien "id" séquentiel 1..N à exposer au front

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    private String question;

    private String option1;
    private String option2;
    private String option3;
    private String option4;
    private String explanation;

    @ElementCollection
    @CollectionTable(
            name = "quiz_question_answers",
            schema = "courses",
            joinColumns = @JoinColumn(name = "question_id")
    )
    @Column(name = "answer", nullable = false, length = 16)
    private List<String> answers = new ArrayList<>();


    public QuizQuestion() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getQuestionNo() {
        return questionNo;
    }

    public void setQuestionNo(Integer questionNo) {
        this.questionNo = questionNo;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getOption1() {
        return option1;
    }

    public void setOption1(String option1) {
        this.option1 = option1;
    }

    public String getOption2() {
        return option2;
    }

    public void setOption2(String option2) {
        this.option2 = option2;
    }

    public String getOption3() {
        return option3;
    }

    public void setOption3(String option3) {
        this.option3 = option3;
    }

    public String getOption4() {
        return option4;
    }

    public void setOption4(String option4) {
        this.option4 = option4;
    }

    public List<String> getAnswers() {
        return answers;
    }

    public void setAnswers(List<String> answers) {
        this.answers = answers;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
