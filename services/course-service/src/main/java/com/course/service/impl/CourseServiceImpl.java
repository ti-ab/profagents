package com.course.service.impl;

import com.course.dto.QuizQuestionDTO;
import com.course.model.Book;
import com.course.model.BookPlan;
import com.course.model.Chapter;
import com.course.model.Section;
import com.course.model.Subchapter;
import com.course.model.Quiz;
import com.course.model.QuizQuestion;
import com.course.repository.BookRepository;
import com.course.service.CourseService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main service responsible for generating and persisting a full course book
 * with GPT models. OpenAI calls are parallelised via virtual threads (Java 21),
 * then the resulting object graph is persisted in PostgreSQL through Spring Data JPA.
 */
@Service
@Transactional
public class CourseServiceImpl implements CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseServiceImpl.class);

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    private final BookRepository bookRepository;

    @Autowired
    public CourseServiceImpl(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    // Inject via application.yml: openai.api.key
    @Value("${openai.api.key}")
    private String openAiApiKey;

    /* ─────────────────────────────────────────────────────────────
       Public API
       ───────────────────────────────────────────────────────────*/

    /**
     * Génère et persiste le livre de cours correspondant à la description fournie.
     * @param bookDescription description du livre/certification (langue = langue de sortie)
     * @return l'entité Book détachée et entièrement sauvegardée.
     */
    @Override
    public Book generateCourseBook(String bookDescription) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("corr", correlationId);
        long t0 = System.nanoTime();

        try {
            log.info("📘 [START] Génération du livre — descriptionLength={} corr={}", bookDescription.length(), correlationId);

            /* 1️⃣ Plan global */
            log.info("🧭 Génération du plan global (bookPlan)...");
            String planJson = callOpenAi(Prompts.bookPlan(bookDescription), "gpt-4o");
            BookPlan plan = JsonUtils.read(planJson, BookPlan.class);
            int totalChapters = plan.chapters() != null ? plan.chapters().size() : 0;
            int totalSubchapters = plan.chapters().stream().mapToInt(c -> c.subchapters().size()).sum();
            int totalSections = plan.chapters().stream()
                    .mapToInt(c -> c.subchapters().stream().mapToInt(sc -> sc.sections().size()).sum())
                    .sum();
            log.info("✅ Plan obtenu: chapters={} subchapters={} sections={}", totalChapters, totalSubchapters, totalSections);

            /* 2️⃣ Racine */
            Book book = new Book(plan.mainTitle(), plan.authors());
            log.info("🪪 Titre='{}' | Auteurs='{}'", plan.mainTitle(), plan.authors());

            /* Contexte de progression partagé */
            Progress progress = new Progress(totalChapters, totalSubchapters, totalSections);

            /* 3️⃣ Chapitres en parallèle (threads virtuels) */
            try (ExecutorService vThreads = Executors.newVirtualThreadPerTaskExecutor()) {
                List<CompletableFuture<Chapter>> futures = new ArrayList<>();

                int i = 0;
                for (BookPlan.ChapterPlan cPlan : plan.chapters()) {
                    final int chapterIndex = i++; // 0-based
                    final String chapterTitle = cPlan.title();
                    futures.add(CompletableFuture.supplyAsync(() -> {
                        MDC.put("corr", correlationId); // propager MDC
                        long tc0 = System.nanoTime();
                        log.info("📂 [CHAP] start idx={} title='{}'", chapterIndex, chapterTitle);
                        try {
                            Chapter c = buildChapter(bookDescription, cPlan, progress);
                            c.setIdx(chapterIndex);
                            log.info("📂 [CHAP] done  idx={} title='{}' elapsedMs={}",
                                    chapterIndex, chapterTitle, msSince(tc0));
                            progress.chaptersDone.incrementAndGet();
                            return c;
                        } catch (RuntimeException e) {
                            log.error("📂 [CHAP] ERROR idx={} title='{}' cause={}", chapterIndex, chapterTitle, e.toString());
                            throw e;
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        } finally {
                            MDC.remove("corr");
                        }
                    }, vThreads));
                }

                futures.forEach(cf -> book.addChapter(cf.join()));
            }

            book.setCreatedAt(LocalDateTime.now());

            /* 4️⃣ Sauvegarde (cascade sur tout l'arbre, y compris Quiz) */
            log.info("💾 Sauvegarde du livre et de l’arbre JPA...");
            Book saved = bookRepository.save(book);
            log.info("🎉 [END] Livre sauvegardé id={} title='{}' totalSections={} elapsedMs={}",
                    saved.getId(), saved.getTitle(), progress.totalSections, msSince(t0));

            return saved;
        } finally {
            MDC.remove("corr");
        }
    }

    /* ─────────────────────────────────────────────────────────────
       Construction d'un chapitre complet (sections + quiz)
       ───────────────────────────────────────────────────────────*/

    private Chapter buildChapter(String description, BookPlan.ChapterPlan plan, Progress progress) throws InterruptedException {
        long t0 = System.nanoTime();

        // 1) Génération du squelette du chapitre
        String chapterJson = callOpenAi(Prompts.chapter(description, plan), "gpt-4o-mini");
        Chapter chapter = JsonUtils.read(chapterJson, Chapter.class);
        chapter.setTitle(plan.title());
        chapter.subchapters().forEach(sc -> sc.setChapter(chapter));

        log.info("📚 [CHAP] Plan détaillé chargé — title='{}' subchapters={}", chapter.getTitle(),
                chapter.subchapters() != null ? chapter.subchapters().size() : 0);

        // Sauvegarde du contexte MDC courant pour les sous-tâches (Structured Concurrency crée des threads virtuels)
        final Map<String, String> parentMdc = MDC.getCopyOfContextMap();

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var subFutures = new ArrayList<StructuredTaskScope.Subtask<Void>>();
            AtomicInteger subIndex = new AtomicInteger(0);

            for (Subchapter sc : chapter.subchapters()) {
                final int scIdx = subIndex.getAndIncrement();
                sc.setIdx(scIdx);
                sc.sections().forEach(s -> s.setSubchapter(sc));

                subFutures.add(scope.fork(() -> {
                    if (parentMdc != null) MDC.setContextMap(parentMdc);
                    long subT0 = System.nanoTime();
                    log.info("📁 [SUB] start chap='{}' idx={} title='{}' sections={}",
                            chapter.getTitle(), scIdx, sc.getTitle(),
                            sc.getSections() != null ? sc.getSections().size() : 0);

                    // Paralléliser les sections de ce subchapter
                    try (var secScope = new StructuredTaskScope.ShutdownOnFailure()) {
                        var secFuts = new ArrayList<StructuredTaskScope.Subtask<Void>>();
                        AtomicInteger secIndex = new AtomicInteger(0);

                        for (Section sec : sc.sections()) {
                            secFuts.add(secScope.fork(() -> {
                                if (parentMdc != null) MDC.setContextMap(parentMdc);

                                final int secIdx = secIndex.getAndIncrement();
                                sec.setIdx(secIdx);

                                final String chapTitle = chapter.getTitle();
                                final String subTitle = sc.getTitle();
                                final String secTitle = sec.getTitle();

                                long ts0 = System.nanoTime();
                                log.info("🧩 [SEC] start chap='{}' sub='{}' idx={} title='{}'",
                                        chapTitle, subTitle, secIdx, secTitle);

                                try {
                                    // 1) Contenu
                                    String contentPrompt = Prompts.section(
                                            description, chapTitle, subTitle, sec
                                    );
                                    String content = callOpenAi(contentPrompt, "gpt-4o-mini");
                                    sec.setContent(content.trim());
                                    log.debug("📝 [SEC] content ok outSize={} elapsedMs={}",
                                            sec.getContent().length(), msSince(ts0));

                                    // 2) Quiz
                                    long tq0 = System.nanoTime();
                                    String quizPrompt = Prompts.sectionQuiz(
                                            description, chapTitle, subTitle, secTitle, sec.getContent()
                                    );
                                    String quizJson = callOpenAi(quizPrompt, "gpt-4o-mini");

                                    List<QuizQuestionDTO> raw = JsonUtils.readList(quizJson, QuizQuestionDTO.class);
                                    int beforeFilter = raw.size();
                                    raw.removeIf(q ->
                                            q == null || q.id() == null || q.answers() == null || q.answers().isEmpty()
                                                    || q.answers().stream().anyMatch(a ->
                                                    !(a.equals("option1") || a.equals("option2") || a.equals("option3") || a.equals("option4")))
                                    );

                                    Quiz quiz = new Quiz();
                                    quiz.setLabel(chapTitle + " / " + subTitle + " / " + secTitle);
                                    List<QuizQuestion> qs = new ArrayList<>();
                                    for (QuizQuestionDTO r : raw) {
                                        QuizQuestion e = new QuizQuestion();
                                        e.setQuestionNo(r.id());
                                        e.setQuestion(r.question());
                                        e.setOption1(r.option1());
                                        e.setOption2(r.option2());
                                        e.setOption3(r.option3());
                                        e.setOption4(r.option4());
                                        e.setAnswers(r.answers());
                                        e.setExplanation(r.explanation());
                                        e.setQuiz(quiz);
                                        qs.add(e);
                                    }
                                    quiz.setQuestions(qs);
                                    sec.setQuiz(quiz);
                                    log.debug("🧪 [SEC] quiz ok questionsBefore={} questionsKept={} elapsedMs={}",
                                            beforeFilter, qs.size(), msSince(tq0));

                                    int done = progress.sectionsDone.incrementAndGet();
                                    long elapsed = msSince(ts0);
                                    double pct = (progress.totalSections > 0)
                                            ? (done * 100.0 / progress.totalSections)
                                            : 100.0;

                                    log.info("✅ [SEC] done chap='{}' sub='{}' idx={} title='{}' contentLen={} quizQ={} elapsedMs={} progress={}/{} ({})",
                                            chapTitle, subTitle, secIdx, secTitle,
                                            sec.getContent().length(), qs.size(), elapsed,
                                            done, progress.totalSections, String.format("%.1f%%", pct));

                                    return null;
                                } catch (RuntimeException ex) {
                                    log.error("⛔ [SEC] error chap='{}' sub='{}' idx={} title='{}' cause={}",
                                            chapTitle, subTitle, secIdx, secTitle, ex.toString());
                                    throw ex;
                                } finally {
                                    MDC.clear();
                                }
                            }));
                        }

                        secScope.join();      // attend toutes les sections
                        secScope.throwIfFailed();

                        log.info("📁 [SUB] done  chap='{}' idx={} title='{}' elapsedMs={}",
                                chapter.getTitle(), scIdx, sc.getTitle(), msSince(subT0));
                        return null;
                    } finally {
                        MDC.clear();
                    }
                }));
            }

            scope.join();           // attend tous les subchapters
            scope.throwIfFailed();  // s’il y a une erreur, annule les sœurs
        } catch (ExecutionException e) {
            log.error("🌩️ [CHAP] subtask failure title='{}' cause={}", chapter.getTitle(), e.toString());
            throw new RuntimeException(e);
        }

        log.info("🏁 [CHAP] fully built title='{}' elapsedMs={}", chapter.getTitle(), msSince(t0));
        return chapter;
    }


    /* ─────────────────────────────────────────────────────────────
       HTTP helper vers OpenAI
       ───────────────────────────────────────────────────────────*/

    private String callOpenAi(String prompt, String model) {
        long t0 = System.nanoTime();
        try {
            HttpClient client = HttpClient.newHttpClient();
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "temperature", 0.7
            );
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_URL))
                    .header("Authorization", "Bearer " + openAiApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                    .build();

            log.debug("↗️  [OpenAI] call model={} promptSize={} promptHash={}…",
                    model, prompt.length(), sha1(prompt));

            HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() != 200) {
                log.error("⛔ [OpenAI] status={} bodySize={} elapsedMs={}",
                        res.statusCode(),
                        res.body() != null ? res.body().length() : 0,
                        msSince(t0));
                throw new IllegalStateException("OpenAI error " + res.statusCode() + " → " + res.body());
            }

            JsonNode root = MAPPER.readTree(res.body());
            String rawContent = root.at("/choices/0/message/content").asText();

            // Nettoyage éventuel de blocs ```json ... ``` ou ``` ... ```
            String cleaned = rawContent
                    .replaceAll("(?i)^(?:```json|```|json)\\s*", "")
                    .replaceAll("```\\s*$", "")
                    .trim();

            log.debug("↘️  [OpenAI] ok model={} outSize={} elapsedMs={} usage.prompt_tokens={} usage.completion_tokens={}",
                    model,
                    cleaned.length(),
                    msSince(t0),
                    root.at("/usage/prompt_tokens").asInt(-1),
                    root.at("/usage/completion_tokens").asInt(-1));

            return cleaned;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("🌩️ [OpenAI] exception={} elapsedMs={}", e.toString(), msSince(t0));
            throw new RuntimeException("Unable to reach OpenAI", e);
        }
    }

    /* ─────────────────────────────────────────────────────────────
       Prompts factory
       ───────────────────────────────────────────────────────────*/

    private static final class Prompts {

        static String bookPlan(String description) {
            return """
                    Can you generate a 200 pages training course book for a certification described as following: "%s".
                    This book contains 400 to 600 words sections that will be read as audio to teach some concepts only orally.
                    The book must only describe the concepts that are questioned during the certification exam (it must not include other topics such as introduction...).
                    Give me the book plan in 1 chapter about the key concept. Each chapter represents a main concept that is usually questioned during the certification exam. Each chapter must contain 4 subchapters. Each subchapter must contain 4 sections.
                    mainTitle represents the main book title in at most 10 words. Authors must be completed only if it is mentioned in the book description.
                    Do not include the chapter number neither the word "chapter" in chapter title. Do not include subchapter number neither the word "subchapter" in subchapter subtitle.
                    You have to answer only in parsable JSON format. Ensure that your answer is parsable in JSON. Answer in the language of the book description.

                    Here is a short example of how the JSON must look like:
                    { "mainTitle": "main title of the book",
                      "authors": "authors of the book",
                      "chapters": [
                        { "title": "Burpees from A to Z",
                          "subchapters": [
                            { "title": "Subchapter 1 example",
                              "sections": ["section 1 example", "section 2 example"]},
                            { "title": "Subchapter 2 example",
                              "sections": ["section 1 example", "section 2 example"]}
                          ]
                        },
                        { "title": "ABC",
                          "subchapters": [
                            { "title": "Subchapter 1 example",
                              "sections": ["section 1 example", "section 2 example"]},
                            { "title": "Subchapter 2 example",
                              "sections": ["section 1 example", "section 2 example"]}
                          ]
                        }
                      ]
                    }
                    """.formatted(description);
        }

        static String chapter(String description, BookPlan.ChapterPlan ch) {
            return """
                    Consider the training course book described as following: "%s". Each chapter has subchapters, and each subchapter has sections.
                    each section must have title and sectionSummary attributes.
                    A section describes in 400 to 600 words a concept to be read and teach only orally.
                    Here is a chapter of the book containing subchapters and sections.
                    You have to answer only in parsable JSON format. Ensure that your answer is parsable in JSON.

                    %s

                    Here is a short example of how the JSON must look like:
                    { "title": "Burpees from A to Z",
                      "subchapters": [
                        { "title": "Subchapter 1 example",
                          "sections": [
                            { "title": "section 1 title example", "sectionSummary": "section 1 summary example"},
                            { "title": "section 2 title example", "sectionSummary": "section 2 summary example"}
                          ]
                        },
                        { "title": "Subchapter 2 example",
                          "sections": [
                            { "title": "section 1 title example", "sectionSummary": "section 1 summary example"},
                            { "title": "section 2 title example", "sectionSummary": "section 2 summary example"}
                          ]
                        }
                      ]
                    }
                    """.formatted(description, JsonUtils.write(ch));
        }

        static String section(String description, String chapTitle, String subTitle, Section s) {
            return """
                    You are writing a section to be read and teach only orally for a training course book described as the following "%s".
                    Can you generate the content of the lesson "%s" from the subchapter "%s" of the chapter "%s" in at least 400 words and at most 600 words ?
                    This section is summarized as the following "%s".
                    Do not include the section title in the output.
                    Answer in the language of the book description.
                    Output must be plain text, not JSON.

                    Here is an example of a section for Java Lambda:

                    The goal of this section is to understand Java lambdas—what they are, when to use them, and a few gotchas—so you can write cleaner, more expressive code.

                    A lambda is a short block of code you can pass around like data. In Java, it implements a functional interface—an interface with exactly one abstract method, like Runnable, Callable, or Comparator<T>. Think of a lambda as an inline method with no name.

                    Syntax, spoken: parameters, arrow, body. Example: “x arrow x times x” represents (x) -> x * x. With a single parameter, you can omit parentheses; with a single expression, you can omit braces and return.

                    Why it’s useful: it removes ceremony from callbacks and collection operations. Instead of creating an anonymous class for a comparator, you can say: “compare by length” → (a, b) -> a.length() - b.length().

                    Quick example—sorting and filtering names. Imagine a list called names. Sorting: “Collections dot sort, names, (a, b) arrow a dot compareToIgnoreCase b.” Filtering with streams: “names dot stream, filter, s arrow s dot startsWith A, collect to list.” You just expressed intent without boilerplate.

                    A lambda can capture variables from the surrounding scope, but only if they’re effectively final—that is, you don’t modify them after assignment. Inside a lambda, the keyword this refers to the enclosing instance, not the lambda itself.

                    Common functional interfaces: Predicate<T> returns boolean, Function<T,R> transforms a value, Consumer<T> performs a side effect, Supplier<T> provides a value, UnaryOperator<T> maps T to T, and BinaryOperator<T> combines two Ts.

                    Pitfalls:

                    Checked exceptions—your lambda must match the functional interface signature; if that method doesn’t declare a checked exception, you can’t throw one without wrapping.

                    Overuse—keep lambdas small and readable; if the body grows, extract a named method.

                    Side effects—prefer pure transformations in streams for clarity and easier testing.

                    Mini-quiz, true or false: “A lambda in Java can implement any interface.” Answer: false—only a functional interface.

                    Challenge for later: refactor a loop that builds a filtered list into a stream().filter(...).collect(...), and replace one anonymous class with a lambda.

                    That’s lambdas: tiny functions, big readability wins.
                    """.formatted(
                    description,
                    s.getTitle(), subTitle, chapTitle,
                    s.getSectionSummary()
            );
        }

        static String sectionQuiz(String description,
                                  String chapTitle,
                                  String subTitle,
                                  String sectionTitle,
                                  String sectionContent) {
            return """
                    You are writing a short quiz for a training course book described as: "%s".
                            Context: chapter "%s" → subchapter "%s" → section "%s".
                            Here is the content of the section (this is the source of truth):
                            ---
                            %s
                            ---
                    
                            Generate a quiz with 4 to 6 questions directly related to this content.
                            The quiz must be returned as **parsable JSON ONLY**: an array of objects, each containing:
                            - id: sequential integer starting from 1
                            - question: the quiz question text
                            - option1, option2, option3, option4: four possible answers (strings)
                            - answers: an array with one or more correct answers (each must be "option1", "option2", "option3", or "option4")
                            - explanation: a detailed explanation of why the listed answers are correct (and why others are not)
                    
                            Rules:
                            - Output must be pure JSON, without any text before or after.
                            - Ensure JSON output is parsable in Java (com.fasterxml.jackson)
                            - Multiple correct answers are allowed if appropriate.
                            - Explanations must be clear, pedagogical, and based on the section content.
                            - All questions and explanations must be in the **same language** as the book description.
                            - Distractors (wrong options) must be plausible but clearly incorrect.
                            - Do not include commas in the question, options, answers and explanation
                    
                            Example of the expected JSON structure:
                    
                            [
                              {
                                "id": 1,
                                "question": "Which of the following are primitive types in Java?",
                                "option1": "int",
                                "option2": "String",
                                "option3": "boolean",
                                "option4": "Integer",
                                "answers": ["option1", "option3"],
                                "explanation": "Primitive types in Java include int and boolean. String and Integer are objects, not primitives."
                              }
                            ]
                    """.formatted(description, chapTitle, subTitle, sectionTitle, sectionContent);
        }
    }

    /* ─────────────────────────────────────────────────────────────
       JSON helpers
       ───────────────────────────────────────────────────────────*/

    private static final class JsonUtils {
        static <T> T read(String j, Class<T> t) {
            try {
                return MAPPER.readValue(j, t);
            } catch (IOException e) {
                throw new IllegalArgumentException("Bad JSON", e);
            }
        }

        static <T> List<T> readList(String j, Class<T> t) {
            try {
                return MAPPER.readValue(
                        j,
                        MAPPER.getTypeFactory().constructCollectionType(List.class, t)
                );
            } catch (IOException e) {
                throw new IllegalArgumentException("Bad JSON list", e);
            }
        }

        static String write(Object v) {
            try {
                return MAPPER.writeValueAsString(v);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /* ─────────────────────────────────────────────────────────────
       Progress helper
       ───────────────────────────────────────────────────────────*/

    private static final class Progress {
        final int totalChapters;
        final int totalSubchapters;
        final int totalSections;
        final AtomicInteger chaptersDone = new AtomicInteger();
        final AtomicInteger subchaptersDone = new AtomicInteger(); // réservé si besoin
        final AtomicInteger sectionsDone = new AtomicInteger();

        Progress(int totalChapters, int totalSubchapters, int totalSections) {
            this.totalChapters = totalChapters;
            this.totalSubchapters = totalSubchapters;
            this.totalSections = totalSections;
        }
    }

    /* ─────────────────────────────────────────────────────────────
       Utils
       ───────────────────────────────────────────────────────────*/

    private static long msSince(long startNs) {
        return Duration.ofNanos(System.nanoTime() - startNs).toMillis();
    }

    private static String sha1(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] d = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "na";
        }
    }
}
