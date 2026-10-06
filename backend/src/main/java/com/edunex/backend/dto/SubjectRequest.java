package com.edunex.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record SubjectRequest(
        @NotBlank String name,
        @NotBlank String code,
        @PositiveOrZero Double coefficient,
        @PositiveOrZero Integer credits
) {
}
