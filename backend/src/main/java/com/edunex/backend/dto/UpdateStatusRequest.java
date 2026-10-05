package com.edunex.backend.dto;

import com.edunex.backend.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull UserStatus status) {
}
