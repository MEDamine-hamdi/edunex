package com.edunex.backend.dto;

public record ProfessorResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String employeeNumber,
        String department,
        String specialty,
        String status
) {
}
