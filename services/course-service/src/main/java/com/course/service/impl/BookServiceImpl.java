package com.course.service.impl;

import com.course.dto.BookDTO;
import com.course.dto.ChapterDTO;
import com.course.dto.QuizDTO;
import com.course.dto.QuizQuestionDTO;
import com.course.dto.SectionDTO;
import com.course.dto.SubchapterDTO;
import com.course.model.Book;
import com.course.model.Quiz;
import com.course.model.Section;
import com.course.repository.BookRepository;
import com.course.service.BookService;
import jakarta.transaction.Transactional;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BookServiceImpl implements BookService {

    @Autowired
    BookRepository bookRepository;

    /* ============================================================
       Helpers de mapping
       ============================================================ */

    private SectionDTO mapSection(Section s) {
        return new SectionDTO(
                s.getId(),
                s.getIdx(),
                s.getTitle(),
                s.getContent(),
                mapQuiz(s.getQuiz())
        );
    }

    private QuizDTO mapQuiz(Quiz q) {
        if (q == null) return null;
        // Si questions est LAZY (ElementCollection), on s'assure de l'init
        Hibernate.initialize(q.getQuestions());
        var questions = q.getQuestions() == null ? List.<QuizQuestionDTO>of()
                : q.getQuestions().stream()
                .map(qq -> new QuizQuestionDTO(
                        qq.getQuestionNo(),
                        qq.getQuestion(),
                        qq.getOption1(),
                        qq.getOption2(),
                        qq.getOption3(),
                        qq.getOption4(),
                        qq.getAnswers(),
                        qq.getExplanation()
                ))
                .toList();

        return new QuizDTO(
                q.getId(),
                q.getLabel(),
                questions
        );
    }

    /* ============================================================
       Méthodes exposées
       ============================================================ */

    /**
     * Version « eager via requête custom » si ton repository
     * charge déjà tout l’arbre (chapters/subchapters/sections).
     * On y ajoute l'init des quiz/questions si nécessaire.
     */
    public List<BookDTO> fullTree() {
        return bookRepository.findAllWithTree().stream().map(b -> new BookDTO(
                b.getId(),
                b.getTitle(),
                b.getAuthors(),
                b.getChapters().stream().map(c -> new ChapterDTO(
                        c.getId(), c.getIdx(), c.getTitle(),
                        c.getSubchapters().stream().map(sc -> new SubchapterDTO(
                                sc.getId(), sc.getIdx(), sc.getTitle(),
                                sc.getSections().stream()
                                        .peek(s -> {
                                            // Sécurise l’init du quiz et des questions
                                            if (s.getQuiz() != null) {
                                                Hibernate.initialize(s.getQuiz());
                                                Hibernate.initialize(s.getQuiz().getQuestions());
                                            }
                                        })
                                        .map(this::mapSection)
                                        .toList()
                        )).toList()
                )).toList()
        )).toList();
    }

    /**
     * Version en chargements paresseux, puis initialisations manuelles.
     */
    public List<BookDTO> fullTreeNotSimultaneous() {

        // 1) books
        List<Book> books = bookRepository.findAll();

        // 2..n) initialisations par niveau
        books.forEach(b -> {
            Hibernate.initialize(b.getChapters());
            b.getChapters().forEach(c -> {
                Hibernate.initialize(c.getSubchapters());
                c.getSubchapters().forEach(sc -> {
                    Hibernate.initialize(sc.getSections());
                    sc.getSections().forEach(s -> {
                        if (s.getQuiz() != null) {
                            Hibernate.initialize(s.getQuiz());
                            Hibernate.initialize(s.getQuiz().getQuestions());
                        }
                    });
                });
            });
        });

        // Mapping DTO
        return books.stream().map(b -> new BookDTO(
                b.getId(),
                b.getTitle(),
                b.getAuthors(),
                b.getChapters().stream().map(c -> new ChapterDTO(
                        c.getId(), c.getIdx(), c.getTitle(),
                        c.getSubchapters().stream().map(sc -> new SubchapterDTO(
                                sc.getId(), sc.getIdx(), sc.getTitle(),
                                sc.getSections().stream()
                                        .map(this::mapSection)
                                        .toList()
                        )).toList()
                )).toList()
        )).toList();
    }

    /**
     * Chargement d’un seul book + init des quiz/questions.
     */
    public BookDTO fullTreeById(Long id) {
        Optional<Book> bookOptional = bookRepository.findById(id);

        if (bookOptional.isEmpty()) {
            return null;
        }

        Book book = bookOptional.get();

        Hibernate.initialize(book.getChapters());
        book.getChapters().forEach(c -> {
            Hibernate.initialize(c.getSubchapters());
            c.getSubchapters().forEach(sc -> {
                Hibernate.initialize(sc.getSections());
                sc.getSections().forEach(s -> {
                    if (s.getQuiz() != null) {
                        Hibernate.initialize(s.getQuiz());
                        Hibernate.initialize(s.getQuiz().getQuestions());
                    }
                });
            });
        });

        return new BookDTO(
                book.getId(),
                book.getTitle(),
                book.getAuthors(),
                book.getChapters().stream().map(c -> new ChapterDTO(
                        c.getId(), c.getIdx(), c.getTitle(),
                        c.getSubchapters().stream().map(sc -> new SubchapterDTO(
                                sc.getId(), sc.getIdx(), sc.getTitle(),
                                sc.getSections().stream()
                                        .map(this::mapSection)
                                        .toList()
                        )).toList()
                )).toList()
        );
    }

    @Override
    public List<BookDTO> listBooksOnly() {

        List<Book> books = bookRepository.findAll();

        return books.stream().map(b -> new BookDTO(
                b.getId(),
                b.getTitle(),
                b.getAuthors(),
                b.getChapters().stream().map(c -> new ChapterDTO(
                        c.getId(), c.getIdx(), c.getTitle(),
                        null
                )).toList()
        )).toList();
    }
}
