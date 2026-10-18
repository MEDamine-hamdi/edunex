package com.edunex.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PermissionDescriptionRequest(@NotNull @Size(max = 255) String description) {
}