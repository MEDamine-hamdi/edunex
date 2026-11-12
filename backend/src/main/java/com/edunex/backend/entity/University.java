package com.edunex.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDateTime;

/** The filter keeps each university's rows apart: tid = 0 means "no restriction". */
@Entity
@Table(name = "universities")
@FilterDef(name = "tenantFilter", defaultCondition = "(:tid = 0 OR university_id = :tid)",
        parameters = @ParamDef(name = "tid", type = Long.class))
@Getter
@Setter
@NoArgsConstructor
public class University {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(length = 100)
    private String city;

    /** ACTIVE or SUSPENDED. */
    @Column(nullable = false, length = 12)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}