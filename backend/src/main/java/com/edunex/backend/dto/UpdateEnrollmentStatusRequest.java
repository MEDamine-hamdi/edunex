package com.edunex.backend.dto;

import com.edunex.backend.enums.EnrollmentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateEnrollmentStatusRequest(@NotNull EnrollmentStatus status) {
}
