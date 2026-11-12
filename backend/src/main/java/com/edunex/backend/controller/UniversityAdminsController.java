package com.edunex.backend.controller;

import com.edunex.backend.dto.CreateAgentRequest;
import com.edunex.backend.service.AdministrationAgentService;
import com.edunex.backend.service.AgentStatusService;
import com.edunex.backend.service.UniversityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** A university administrator creates more administrators of the SAME university. */
@RestController
@RequestMapping("/api/admin/agents")
@PreAuthorize("hasRole('ADMIN_AGENT') and hasAuthority('ACCOUNTS_MANAGE')")
@RequiredArgsConstructor
public class UniversityAdminsController {

    public record StatusRequest(@NotBlank String status) {
    }

    private final UniversityService universityService;
    private final AdministrationAgentService agentService;
    private final AgentStatusService agentStatus;

    @GetMapping
    public List<UniversityService.AdminRow> list() {
        return universityService.adminsHere();
    }

    @PostMapping
    public ResponseEntity<Object> create(@Valid @RequestBody CreateAgentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(agentService.create(request));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Void> status(@PathVariable Long id, @Valid @RequestBody StatusRequest request, Authentication auth) {
        agentStatus.setStatus(id, request.status(), auth.getName());
        return ResponseEntity.noContent().build();
    }
}