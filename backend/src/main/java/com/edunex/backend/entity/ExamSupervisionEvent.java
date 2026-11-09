package com.edunex.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** The history: every registration, assignment, withdrawal and removal, with who did it. */
@Entity
@Table(name = "exam_supervision_events", indexes = @Index(name = "idx_exam_events_prof", columnList = "professor_id"))
@Getter
@Setter
@NoArgsConstructor
public class ExamSupervisionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_id")
    private Long examId;

    @Column(name = "exam_label", length = 300)
    private String examLabel;

    @Column(name = "professor_id", nullable = false)
    private Long professorId;

    @Column(name = "professor_name", length = 200)
    private String professorName;

    /** VOLUNTEERED, ASSIGNED, WITHDRAWN, REMOVED or EXAM_CANCELLED. */
    @Column(nullable = false, length = 20)
    private String action;

    @Column(length = 200)
    private String actor;

    @Column(length = 500)
    private String reason;

    @Column(name = "at_time", nullable = false)
    private LocalDateTime at = LocalDateTime.now();
}