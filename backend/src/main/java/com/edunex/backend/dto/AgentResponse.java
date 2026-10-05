package com.edunex.backend.dto;

public record AgentResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String employeeNumber,
        String service,
        String status
) {
}
