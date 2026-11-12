package com.edunex.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class Conversation extends com.edunex.backend.tenant.TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** DIRECT or GROUP. */
    @Column(nullable = false, length = 10)
    private String type;

    @Column(length = 100)
    private String name;

    @Column(name = "photo_key", length = 500)
    private String photoKey;

    /** "smallerId-largerId" for a private conversation: one per pair. */
    @Column(name = "direct_key", length = 40, unique = true)
    private String directKey;

    @Column(name = "created_by_id")
    private Long createdById;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }
}