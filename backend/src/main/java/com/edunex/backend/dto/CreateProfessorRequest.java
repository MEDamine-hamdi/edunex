package com.edunex.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateProfessorRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        String phone,
        @NotBlank String employeeNumber,
        String department,
        String specialty,
        String professorType,
        @NotBlank @Pattern(regexp = "^\\d{8}$", message = "must be 8 digits") String cin
) {
}