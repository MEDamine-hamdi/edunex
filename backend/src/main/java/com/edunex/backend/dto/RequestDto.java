package com.edunex.backend.dto;

import com.edunex.backend.enums.RequestStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class RequestDto {

    private RequestDto() {
    }

    public record Create(@NotBlank String type, @Min(1) @Max(10) int copies,
                         @NotBlank @Size(min = 5, max = 1000) String reason, @Size(max = 1000) String details) {
    }

    public record Update(@NotNull RequestStatus status, @Size(max = 1000) String note) {
    }

    public record View(Long id, String type, int copies, String reason, String details, String status,
                       String adminNote, String handledBy, String requesterName, String requesterRole,
                       String requesterEmail, LocalDateTime createdAt, LocalDateTime updatedAt,
                       boolean hasFile, String fileName) {
    }
}