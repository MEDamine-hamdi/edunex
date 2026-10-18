package com.edunex.backend.dto;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ProfileUpdateRequest(
        @Size(max = 30) String phone,
        @Size(max = 255) String address,
        @Past LocalDate dateOfBirth
) {
}