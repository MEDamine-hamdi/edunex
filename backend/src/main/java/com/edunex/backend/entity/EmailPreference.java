package com.edunex.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A user who turned e-mails off. No row means e-mails are on. */
@Entity
@Table(name = "email_preferences")
@Getter
@Setter
@NoArgsConstructor
public class EmailPreference {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private boolean enabled = true;
}