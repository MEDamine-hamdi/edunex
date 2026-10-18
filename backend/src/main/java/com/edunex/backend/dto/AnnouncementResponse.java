package com.edunex.backend.dto;

import java.time.LocalDateTime;

public record AnnouncementResponse(
        Long id,
        String title,
        String content,
        LocalDateTime publishedAt,
        String audience,
        Long classId,
        String className,
        String publishedBy,
        Integer recipients
) {
}