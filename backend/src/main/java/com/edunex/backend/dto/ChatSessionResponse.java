package com.edunex.backend.dto;

import java.time.LocalDateTime;

public record ChatSessionResponse(Long id, String title, LocalDateTime createdAt, LocalDateTime updatedAt) {
}