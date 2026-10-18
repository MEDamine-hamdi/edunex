package com.edunex.backend.dto;

import java.time.LocalDateTime;

public record ActivityLogResponse(
        Long id,
        String action,
        String entityType,
        LocalDateTime timestamp,
        String ipAddress,
        String userEmail,
        String userName
) {
}