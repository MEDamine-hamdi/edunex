package com.edunex.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CourseResponse(
        Long id,
        String title,
        String description,
        LocalDateTime createdAt,
        SubjectRef subject,
        String professorName,
        List<DocumentInfo> documents
) {
    public record SubjectRef(Long id, String name, String code) {
    }

    public record DocumentInfo(Long id, String title, String type, Long size, LocalDateTime uploadedAt) {
    }
}
