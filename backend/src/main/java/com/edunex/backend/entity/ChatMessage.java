package com.edunex.backend.entity;

import com.edunex.backend.enums.SenderType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SenderType sender;

    /** Cited documents, one per line, fields separated by tabs. */
    @Column(columnDefinition = "TEXT")
    private String sources;

    /** Name of the file the student attached to this message, if any. */
    @Column(name = "attachment_name", length = 255)
    private String attachmentName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_session_id", nullable = false)
    private ChatSession chatSession;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}