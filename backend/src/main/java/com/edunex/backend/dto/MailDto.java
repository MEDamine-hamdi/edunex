package com.edunex.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.List;

public final class MailDto {

    private MailDto() {
    }

    public record Failure(String to, String subject, String error, LocalDateTime createdAt) {
    }

    public record Status(boolean configured, String senderEmail, long pending, long sent, long failed,
                         List<Failure> recentFailures) {
    }

    public record TestRequest(@NotBlank @Email String to) {
    }
}