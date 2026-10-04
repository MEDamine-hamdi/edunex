package com.edunex.backend.dto;

public record LoginResponse(
        String token,
        String type,
        Long id,
        String email,
        String firstName,
        String lastName,
        String role
) {
}
