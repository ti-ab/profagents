
package com.course.controller;

import com.course.dto.BookDTO;
import com.course.model.Book;
import com.course.repository.BookRepository;
import com.course.service.BookService;
import com.course.service.CourseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
public class BookController {

    private final BookService bookService;
    private final CourseService courseService;
    private final BookRepository bookRepository;

    public BookController(BookService bookService, CourseService courseService, BookRepository bookRepository) {
        this.bookService = bookService;
        this.courseService = courseService;
        this.bookRepository = bookRepository;
    }

    /* ===== Existing reads ===== */
    @GetMapping("/api/books")
    public List<BookDTO> listBooks() {
        return bookService.fullTreeNotSimultaneous();
    }

    @GetMapping("/api/booksOnly")
    public List<BookDTO> listBooksOnly() {
        return bookService.listBooksOnly();
    }

    @GetMapping("/api/books/{id}")
    public BookDTO getBookById(@PathVariable Long id) {
        return bookService.fullTreeById(id);
    }

    /* ===== Generate a book from description ===== */
    public record GenerateRequest(String description, String title, String authors) {}

    @PostMapping("/api/books/generate")
    public ResponseEntity<BookDTO> generate(@RequestBody GenerateRequest req) {
        String description = (req != null && req.description() != null && !req.description().isBlank())
                ? req.description()
                : "Untitled course";
        Book created = courseService.generateCourseBook(description);
        if (req != null) {
            if (req.title() != null && !req.title().isBlank()) {
                created.setTitle(req.title());
            }
            if (req.authors() != null && !req.authors().isBlank()) {
                created.setAuthors(req.authors());
            }
            created = bookRepository.save(created);
        }
        // return minimal DTO (id/title/authors)
        return ResponseEntity.ok(new BookDTO(created.getId(), created.getTitle(), created.getAuthors(), List.of()));
    }

    /* ===== Basic CRUD on Book metadata ===== */
    @PostMapping("/api/books")
    public ResponseEntity<Book> create(@RequestBody Book book) {
        // Note: creating manually won't build chapters/sections; use /generate for full books
        return ResponseEntity.ok(bookRepository.save(book));
    }

    @PutMapping("/api/books/{id}")
    public ResponseEntity<Book> update(@PathVariable Long id, @RequestBody Book incoming) {
        Optional<Book> opt = bookRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Book b = opt.get();
        if (incoming.getTitle() != null) b.setTitle(incoming.getTitle());
        if (incoming.getAuthors() != null) b.setAuthors(incoming.getAuthors());
        return ResponseEntity.ok(bookRepository.save(b));
    }

    @DeleteMapping("/api/books/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!bookRepository.existsById(id)) return ResponseEntity.notFound().build();
        bookRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
