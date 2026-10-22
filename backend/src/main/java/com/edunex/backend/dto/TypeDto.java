package com.edunex.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class TypeDto {

    private TypeDto() {
    }

    public record View(Long id, String name, String audience, boolean active, boolean requiresDetails, int maxCopies) {
    }

    public record Save(@NotBlank @Size(max = 120) String name, @NotBlank String audience, boolean active,
                       boolean requiresDetails, @Min(1) @Max(10) int maxCopies) {
    }

    public record Option(String name, boolean requiresDetails, int maxCopies) {
    }
}