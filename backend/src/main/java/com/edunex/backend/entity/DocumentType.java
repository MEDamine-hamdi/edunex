package com.edunex.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A kind of document that students and/or professors may request. Managed by the administration. */
@Entity
@Table(name = "document_types", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
@Getter
@Setter
@NoArgsConstructor
public class DocumentType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    /** STUDENT, PROFESSOR or BOTH. */
    @Column(nullable = false, length = 12)
    private String audience = "STUDENT";

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "requires_details", nullable = false)
    private boolean requiresDetails;

    @Column(name = "max_copies", nullable = false)
    private int maxCopies = 5;
}