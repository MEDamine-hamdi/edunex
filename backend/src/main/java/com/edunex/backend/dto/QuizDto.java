package com.edunex.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class QuizDto {

    private QuizDto() {
    }

    public record Info(Long courseId, String title, String subjectName, boolean completed, LocalDateTime completedAt,
                       Integer lastScore, Integer lastTotal, int attempts) {
    }

    public record QuestionView(int index, String question, List<String> options) {
    }

    public record QuizView(Long id, Long courseId, String courseTitle, List<QuestionView> questions) {
    }

    public record SubmitRequest(@NotNull @Size(max = 20) List<Integer> answers) {
    }

    public record ResultItem(int index, String question, List<String> options, Integer chosen, int correct,
                             boolean right, String explanation) {
    }

    public record QuizResult(Long id, Long courseId, String courseTitle, int score, int total,
                             LocalDateTime submittedAt, List<ResultItem> items) {
    }

    public record AttemptRow(Long id, LocalDateTime createdAt, Integer score, int total, boolean submitted) {
    }

    public record QuizState(QuizView quiz, QuizResult result) {
    }
}