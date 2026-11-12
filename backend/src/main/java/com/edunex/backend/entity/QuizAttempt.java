package com.edunex.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** One generated quiz of a student for a course. The correct answers stay on the server until the quiz is submitted. */
@Entity
@Table(name = "quiz_attempts", indexes = @Index(name = "idx_quiz_student_course", columnList = "student_id, course_id"))
@Getter
@Setter
@NoArgsConstructor
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "questions_json", nullable = false, columnDefinition = "TEXT")
    private String questionsJson;

    @Column(name = "answers_json", columnDefinition = "TEXT")
    private String answersJson;

    private Integer score;

    @Column(nullable = false)
    private int total;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
}