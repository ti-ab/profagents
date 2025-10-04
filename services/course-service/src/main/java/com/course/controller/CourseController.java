package com.course.controller;

import com.course.model.Book;
import com.course.repository.BookRepository;
import com.course.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CourseController {

    @Autowired
    CourseService courseService;

    @GetMapping("/api/courses")
    public String getCourses() {
        return "Liste des cours IA!!";
    }

    @GetMapping("/api/generate")
    public String generateCourseBook(@RequestParam String bookDescription) {


        courseService.generateCourseBook(bookDescription);

        return "Génération réussie!!";


    }



}
