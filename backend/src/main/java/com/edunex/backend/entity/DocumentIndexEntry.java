package com.edunex.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Indexing state of one course document: INDEXED, EMPTY (no text), UNSUPPORTED or FAILED. */
@Entity
@Table(name = "document_index")
@Getter
@Setter
@NoArgsConstructor
public class DocumentIndexEntry {

    @Id
    @Column(name = "document_id")
    private Long documentId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @Column(name = "indexed_at", nullable = false)
    private LocalDateTime indexedAt;
}