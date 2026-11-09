package com.edunex.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateAgentRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        String phone,
        @NotBlank String employeeNumber,
        String service,
        @NotBlank @Pattern(regexp = "^\\d{8}$", message = "must be 8 digits") String cin
) {
}