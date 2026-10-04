package com.edunex.backend.dto;

public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phone,
        String role,
        String status
) {
}
