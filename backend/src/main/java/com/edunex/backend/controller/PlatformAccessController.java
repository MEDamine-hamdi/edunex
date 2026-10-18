package com.edunex.backend.controller;

import com.edunex.backend.dto.AssignIdsRequest;
import com.edunex.backend.dto.PermissionDescriptionRequest;
import com.edunex.backend.dto.PermissionResponse;
import com.edunex.backend.dto.RoleResponse;
import com.edunex.backend.service.AccessControlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/platform")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@RequiredArgsConstructor
public class PlatformAccessController {

    private final AccessControlService accessControlService;

    @GetMapping("/roles")
    public List<RoleResponse> roles() {
        return accessControlService.roles();
    }

    @PutMapping("/roles/{id}/permissions")
    public RoleResponse assign(@PathVariable Long id, @Valid @RequestBody AssignIdsRequest request) {
        return accessControlService.assignPermissions(id, request.ids());
    }

    @GetMapping("/permissions")
    public List<PermissionResponse> permissions() {
        return accessControlService.permissions();
    }

    @PutMapping("/permissions/{id}")
    public PermissionResponse updateDescription(@PathVariable Long id,
                                                @Valid @RequestBody PermissionDescriptionRequest request) {
        return accessControlService.updateDescription(id, request.description());
    }
}