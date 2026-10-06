package com.edunex.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record TimetableRequest(
        @NotBlank String academicYear,
        @NotBlank String semester
) {
}
