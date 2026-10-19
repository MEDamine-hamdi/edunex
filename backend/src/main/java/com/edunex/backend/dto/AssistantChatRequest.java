package com.edunex.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** subjectId / courseId only matter when a new conversation starts; an existing conversation keeps its own scope. */
public record AssistantChatRequest(
        Long sessionId,
        Long subjectId,
        Long courseId,
        @NotBlank @Size(max = 2000) String message
) {
}