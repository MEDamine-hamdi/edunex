package com.edunex.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** One counter per key (account, account+IP, e-mail request...). Kept in the database: it survives restarts. */
@Entity
@Table(name = "login_throttle")
@Getter
@Setter
@NoArgsConstructor
public class LoginThrottle {

    @Id
    @Column(name = "throttle_key", length = 320)
    private String id;

    @Column(nullable = false)
    private int failures;

    @Column(name = "window_start")
    private Instant windowStart;

    @Column(name = "locked_until")
    private Instant lockedUntil;
}