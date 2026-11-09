package com.edunex.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Absences allowed before elimination. subjectId null = the default for every subject. */
@Entity
@Table(name = "absence_policies")
@Getter
@Setter
@NoArgsConstructor
public class AbsencePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subject_id")
    private Long subjectId;

    @Column(name = "max_absences", nullable = false)
    private int maxAbsences;

    /** How many lates count as one absence (0 = lates are ignored). */
    @Column(name = "lates_per_absence", nullable = false)
    private int latesPerAbsence;
}