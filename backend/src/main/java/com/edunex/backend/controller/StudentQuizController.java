package com.edunex.backend.controller;

import com.edunex.backend.dto.QuizDto;
import com.edunex.backend.exception.AiUnavailableException;
import com.edunex.backend.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentQuizController {

    private final QuizService service;

    @GetMapping("/courses/{courseId}/quiz-info")
    public QuizDto.Info info(@PathVariable Long courseId, Authentication auth) {
        return service.info(auth.getName(), courseId);
    }

    @PostMapping("/courses/{courseId}/complete")
    public ResponseEntity<Void> complete(@PathVariable Long courseId, Authentication auth) {
        service.complete(auth.getName(), courseId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/courses/{courseId}/quizzes")
    public ResponseEntity<QuizDto.QuizView> generate(@PathVariable Long courseId, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.generate(auth.getName(), courseId));
    }

    @GetMapping("/courses/{courseId}/quizzes")
    public List<QuizDto.AttemptRow> history(@PathVariable Long courseId, Authentication auth) {
        return service.history(auth.getName(), courseId);
    }

    @GetMapping("/quizzes/{id}")
    public QuizDto.QuizState state(@PathVariable Long id, Authentication auth) {
        return service.state(auth.getName(), id);
    }

    @PostMapping("/quizzes/{id}/submit")
    public QuizDto.QuizResult submit(@PathVariable Long id, @Valid @RequestBody QuizDto.SubmitRequest request, Authentication auth) {
        return service.submit(auth.getName(), id, request.answers());
    }

    @ExceptionHandler(AiUnavailableException.class)
    public ResponseEntity<Map<String, String>> unavailable(AiUnavailableException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
    }
}