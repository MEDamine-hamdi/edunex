package com.edunex.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ChatMessageResponse(Long id, String role, String content, LocalDateTime createdAt, List<SourceRef> sources) {
}