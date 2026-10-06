package com.edunex.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateEnrollmentRequest(
        @NotNull Long studentId,
        @NotNull Long classId,
        Long sectionId,
        LocalDate enrollmentDate
) {
}
