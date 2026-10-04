package com.edunex.backend.entity;

import com.edunex.backend.enums.TargetAudience;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "announcements")
@Getter
@Setter
@NoArgsConstructor
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "published_at", nullable = false, updatable = false)
    private LocalDateTime publishedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_audience", nullable = false, length = 20)
    private TargetAudience targetAudience = TargetAudience.ALL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_class_id")
    private SchoolClass targetClass;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "published_by", nullable = false)
    private AdministrationAgent publishedBy;

    @PrePersist
    protected void onCreate() {
        this.publishedAt = LocalDateTime.now();
    }
}
