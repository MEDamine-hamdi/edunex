package com.edunex.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** An absence that an approved certificate removed: kept so that a revocation can bring it back. */
@Entity
@Table(name = "certificate_links", indexes = @Index(name = "idx_cert_link_cert", columnList = "certificate_id"))
@Getter
@Setter
@NoArgsConstructor
public class CertificateLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "certificate_id", nullable = false)
    private Long certificateId;

    @Column(name = "attendance_id", nullable = false)
    private Long attendanceId;

    @Column(name = "previous_status", nullable = false, length = 20)
    private String previousStatus;

    @Column(name = "previous_justification", length = 500)
    private String previousJustification;
}