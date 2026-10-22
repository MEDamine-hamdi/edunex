package com.edunex.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A professor teaching a subject in one specific class. */
@Entity
@Table(name = "class_teachings", uniqueConstraints = @UniqueConstraint(columnNames = {"class_id", "subject_id", "professor_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ClassTeaching {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "professor_id", nullable = false)
    private Long professorId;
}