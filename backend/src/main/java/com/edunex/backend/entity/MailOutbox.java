package com.edunex.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** E-mails waiting to be sent. Written in the same transaction as the notification they announce. */
@Entity
@Table(name = "mail_outbox", indexes = @Index(name = "idx_mail_outbox_status", columnList = "status, next_attempt_at"))
@Getter
@Setter
@NoArgsConstructor
public class MailOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "to_email", nullable = false, length = 200)
    private String toEmail;

    @Column(name = "to_name", length = 200)
    private String toName;

    @Column(nullable = false, length = 250)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String html;

    /** PENDING, SENT or FAILED. */
    @Column(nullable = false, length = 12)
    private String status = "PENDING";

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (nextAttemptAt == null) {
            nextAttemptAt = createdAt;
        }
    }
}