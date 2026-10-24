package com.edunex.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** A single-use link sent by e-mail (password reset or e-mail change). Only the hash of the token is stored. */
@Entity
@Table(name = "account_tokens", indexes = @Index(name = "idx_account_tokens_user", columnList = "user_id, type"))
@Getter
@Setter
@NoArgsConstructor
public class AccountToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** PASSWORD_RESET or EMAIL_CHANGE. */
    @Column(nullable = false, length = 20)
    private String type;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "new_email", length = 254)
    private String newEmail;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}