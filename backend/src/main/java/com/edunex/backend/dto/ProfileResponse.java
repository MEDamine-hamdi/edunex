package com.edunex.backend.dto;

import java.time.LocalDate;
import java.util.List;

public record ProfileResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String role,
        String status,
        String address,
        LocalDate dateOfBirth,
        boolean hasPhoto,
        List<Detail> details
) {
    public record Detail(String label, String value) {
    }
}