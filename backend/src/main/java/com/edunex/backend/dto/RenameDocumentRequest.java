package com.edunex.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record RenameDocumentRequest(@NotBlank String title) {
}
