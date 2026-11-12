package com.edunex.backend.controller;

import com.edunex.backend.dto.CreateAgentRequest;
import com.edunex.backend.service.UniversityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** The platform administrator manages universities and their first administrators. */
@RestController
@RequestMapping("/api/platform/universities")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@RequiredArgsConstructor
public class UniversityController {

    public record StatusRequest(@NotBlank String status) {
    }

    private final UniversityService service;

    @GetMapping
    public List<UniversityService.Row> list() {
        return service.list();
    }

    @PostMapping
    public ResponseEntity<UniversityService.Row> create(@Valid @RequestBody UniversityService.SaveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Void> status(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        service.setStatus(id, request.status());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/admins")
    public List<UniversityService.AdminRow> admins(@PathVariable Long id) {
        return service.admins(id);
    }

    @PostMapping("/{id}/admins")
    public ResponseEntity<Object> createAdmin(@PathVariable Long id, @Valid @RequestBody CreateAgentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createAdmin(id, request));
    }

    @PutMapping("/{id}/admins/{agentId}/status")
    public ResponseEntity<Void> adminStatus(@PathVariable Long id, @PathVariable Long agentId, @Valid @RequestBody StatusRequest request) {
        service.setAdminStatus(id, agentId, request.status());
        return ResponseEntity.noContent().build();
    }
}