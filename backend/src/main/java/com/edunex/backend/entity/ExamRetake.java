package com.edunex.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** The right of a student to redo an exam he could not take (medical certificate). */
@Entity
@Table(name = "exam_retakes", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "exam_id"}))
@Getter
@Setter
@NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class ExamRetake extends com.edunex.backend.tenant.TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "certificate_id")
    private Long certificateId;

    /** TO_SCHEDULE, SCHEDULED, DONE or CANCELLED. */
    @Column(nullable = false, length = 12)
    private String status = "TO_SCHEDULE";

    @Column(name = "retake_date")
    private LocalDate retakeDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(length = 100)
    private String room;

    @Column(name = "scheduled_by", length = 200)
    private String scheduledBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}