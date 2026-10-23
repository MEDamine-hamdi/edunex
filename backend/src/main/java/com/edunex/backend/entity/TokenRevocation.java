package com.edunex.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Tokens issued before valid_after are refused for this account. */
@Entity
@Table(name = "token_revocations")
@Getter
@Setter
@NoArgsConstructor
public class TokenRevocation {

    @Id
    @Column(length = 200)
    private String email;

    @Column(name = "valid_after", nullable = false)
    private Instant validAfter;
}