package com.edunex.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** The defense (soutenance) of an internship: date, jury, result and the two documents. */
@Entity
@Table(name = "defenses")
@Getter
@Setter
@NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class Defense extends com.edunex.backend.tenant.TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "internship_id", nullable = false, unique = true)
    private Internship internship;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(length = 100)
    private String room;

    /** PLANNED or DONE. */
    @Column(nullable = false, length = 12)
    private String status = "PLANNED";

    private Double grade;

    @Column(length = 2000)
    private String comments;

    /** Final version of the internship report (PDF). */
    @Column(name = "report_key", length = 500)
    private String reportKey;

    @Column(name = "report_name", length = 255)
    private String reportName;

    /** The paper where the result is written (scan or PDF). */
    @Column(name = "record_key", length = 500)
    private String recordKey;

    @Column(name = "record_name", length = 255)
    private String recordName;

    @OneToMany(mappedBy = "defense", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DefenseJuror> jurors = new ArrayList<>();
}