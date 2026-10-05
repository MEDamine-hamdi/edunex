package com.edunex.backend.controller;

import com.edunex.backend.dto.AgentResponse;
import com.edunex.backend.dto.CreateAgentRequest;
import com.edunex.backend.dto.UpdateStatusRequest;
import com.edunex.backend.service.AdministrationAgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/platform/agents")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@RequiredArgsConstructor
public class PlatformAgentController {

    private final AdministrationAgentService agentService;

    @PostMapping
    public ResponseEntity<AgentResponse> create(@Valid @RequestBody CreateAgentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(agentService.create(request));
    }

    @GetMapping
    public List<AgentResponse> list() {
        return agentService.findAll();
    }

    @PatchMapping("/{id}/status")
    public AgentResponse updateStatus(@PathVariable Long id,
                                      @Valid @RequestBody UpdateStatusRequest request) {
        return agentService.updateStatus(id, request.status());
    }
}
