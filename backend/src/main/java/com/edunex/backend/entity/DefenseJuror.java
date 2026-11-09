package com.edunex.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "defense_jurors", uniqueConstraints = @UniqueConstraint(columnNames = {"defense_id", "professor_id"}))
@Getter
@Setter
@NoArgsConstructor
public class DefenseJuror {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "defense_id", nullable = false)
    private Defense defense;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professor_id", nullable = false)
    private Professor professor;

    /** PRESIDENT, RAPPORTEUR or MEMBER. */
    @Column(nullable = false, length = 12)
    private String role;
}