package com.edunex.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** PERMANENT or VACATAIRE. No row means PERMANENT. */
@Entity
@Table(name = "professor_kinds")
@Getter
@Setter
@NoArgsConstructor
public class ProfessorKind {

    @Id
    @Column(name = "professor_id")
    private Long professorId;

    @Column(nullable = false, length = 12)
    private String kind;
}