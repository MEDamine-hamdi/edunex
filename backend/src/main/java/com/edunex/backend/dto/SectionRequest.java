package com.edunex.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record SectionRequest(@NotBlank String name) {
}
