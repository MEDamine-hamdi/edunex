package com.edunex.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** One occurrence (a date) of a weekly timetable session that does not take place because the teacher is absent. */
@Entity
@Table(name = "session_absences",
        uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "absence_date"}))
@Getter
@Setter
@NoArgsConstructor
public class SessionAbsence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "absence_date", nullable = false)
    private LocalDate date;

    /** PROFESSOR (declared by the teacher) or ADMIN (marked by the administration). */
    @Column(nullable = false, length = 12)
    private String kind;

    @Column(length = 500)
    private String reason;

    @Column(name = "declared_by_id")
    private Long declaredById;

    @Column(name = "declared_by", length = 200)
    private String declaredBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}