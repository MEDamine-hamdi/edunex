package com.edunex.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One right added to (GRANT) or taken from (DENY) a specific person, on top of the rights of the role. */
@Entity
@Table(name = "user_permissions", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "code"}))
@Getter
@Setter
@NoArgsConstructor
public class UserPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 60)
    private String code;

    /** GRANT or DENY. */
    @Column(nullable = false, length = 5)
    private String effect;
}