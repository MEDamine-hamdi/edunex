package com.edunex.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** A medical certificate that justifies the absences (sessions and exams) of a student over a period. */
@Entity
@Table(name = "medical_certificates", indexes = @Index(name = "idx_cert_student", columnList = "student_id"))
@Getter
@Setter
@NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class MedicalCertificate extends com.edunex.backend.tenant.TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /** APP (sent through the app, must be verified) or IN_PERSON (handed to the administration). */
    @Column(nullable = false, length = 10)
    private String source;

    /** PENDING, APPROVED, REJECTED, REVOKED or WITHDRAWN. */
    @Column(nullable = false, length = 10)
    private String status = "PENDING";

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(length = 1000)
    private String note;

    /** Ids of the exams the student says were missed, separated by commas. */
    @Column(name = "exam_ids", length = 500)
    private String examIds;

    @Column(name = "file_key", length = 500)
    private String fileKey;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "created_by", length = 200)
    private String createdBy;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "decided_by", length = 200)
    private String decidedBy;

    @Column(name = "decision_note", length = 1000)
    private String decisionNote;
}