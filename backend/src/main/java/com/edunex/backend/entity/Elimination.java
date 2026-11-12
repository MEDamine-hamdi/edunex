package com.edunex.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "eliminations", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "subject_id"}))
@Getter
@Setter
@NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class Elimination extends com.edunex.backend.tenant.TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(nullable = false)
    private int absences;

    @Column(name = "max_absences", nullable = false)
    private int maxAbsences;

    @Column(name = "eliminated_at", nullable = false)
    private LocalDateTime eliminatedAt = LocalDateTime.now();

    @Column(name = "waived_at")
    private LocalDateTime waivedAt;

    @Column(name = "waived_by", length = 200)
    private String waivedBy;

    @Column(name = "waive_reason", length = 500)
    private String waiveReason;
}