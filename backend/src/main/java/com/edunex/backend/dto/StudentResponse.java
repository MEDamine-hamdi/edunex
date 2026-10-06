package com.edunex.backend.dto;

public record StudentResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String studentNumber,
        Integer enrollmentYear,
        String level,
        String specialty,
        String status
) {
}
