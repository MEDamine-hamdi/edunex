package com.edunex.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record CourseRequest(
        @NotBlank String title,
        String description,
        Long subjectId
) {
}
