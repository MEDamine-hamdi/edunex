package com.edunex.backend.dto;

import com.edunex.backend.enums.TargetAudience;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AnnouncementRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String content,
        @NotNull TargetAudience audience,
        Long classId
) {
}