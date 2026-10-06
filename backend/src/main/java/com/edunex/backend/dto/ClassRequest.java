package com.edunex.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record ClassRequest(
        @NotBlank String name,
        String level,
        @NotBlank String academicYear
) {
}
