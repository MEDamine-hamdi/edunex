package com.edunex.backend.dto;

import java.util.List;

public record PermissionResponse(Long id, String code, String description, List<String> roles) {
}