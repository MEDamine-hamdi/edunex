package com.edunex.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** A source of the institute guide: an uploaded file or a web link. */
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

    /** Null for links. */
    @Column(name = "file_key", length = 500)
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

    /** FILE or LINK; null (older rows) means FILE. */
    @Column(name = "source_kind", length = 10)
    private String sourceKind;

    @Column(length = 1000)
    private String url;

    @Column(name = "last_fetched_at")
    private LocalDateTime lastFetchedAt;

    @Column(name = "last_error", length = 300)
    private String lastError;

    public boolean isLink() {
        return "LINK".equals(sourceKind);
    }

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
    }
}