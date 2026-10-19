package com.edunex.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** A document of the institute guide (rules, procedures, calendar, contacts) used by the general assistant. */
@Entity
@Table(name = "institute_documents")
@Getter
@Setter
@NoArgsConstructor
public class InstituteDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "file_key", nullable = false, length = 500)
    private String fileKey;

    @Column(length = 20)
    private String type;

    @Column(name = "file_size")
    private Long size;

    /** PENDING, INDEXED, EMPTY (no text found), UNSUPPORTED or FAILED. */
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @Column(name = "uploaded_by", length = 200)
    private String uploadedBy;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
    }
}