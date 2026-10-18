package com.edunex.backend.dto;

import java.util.List;

public record RoleResponse(
        Long id,
        String name,
        String description,
        boolean locked,
        long userCount,
        List<PermissionRef> permissions
) {
    public record PermissionRef(Long id, String code, String description) {
    }
}