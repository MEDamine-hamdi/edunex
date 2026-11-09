package com.edunex.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** A professor who must be present at an exam. Only active supervisions are stored here. */
@Entity
@Table(name = "exam_supervisions", uniqueConstraints = @UniqueConstraint(columnNames = {"exam_id", "professor_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ExamSupervision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professor_id", nullable = false)
    private Professor professor;

    /** ASSIGNED (by the administration) or VOLUNTEER (chosen by a vacataire). */
    @Column(nullable = false, length = 12)
    private String mode;

    @Column(name = "assigned_by", length = 200)
    private String assignedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}