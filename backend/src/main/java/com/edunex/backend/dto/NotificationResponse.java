package com.edunex.backend.dto;

import java.time.LocalDateTime;

public record NotificationResponse(Long id, String message, String type, boolean read, LocalDateTime sentAt) {
}