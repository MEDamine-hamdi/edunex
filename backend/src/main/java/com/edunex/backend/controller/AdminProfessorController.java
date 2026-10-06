package com.edunex.backend.controller;

import com.edunex.backend.dto.CreateProfessorRequest;
import com.edunex.backend.dto.ProfessorResponse;
import com.edunex.backend.dto.UpdateStatusRequest;
import com.edunex.backend.service.ProfessorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/professors")
@PreAuthorize("hasRole('ADMIN_AGENT')")
@RequiredArgsConstructor
public class AdminProfessorController {

    private final ProfessorService professorService;

    @PostMapping
    public ResponseEntity<ProfessorResponse> create(@Valid @RequestBody CreateProfessorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(professorService.create(request));
    }

    @GetMapping
    public List<ProfessorResponse> list() {
        return professorService.findAll();
    }

    @PatchMapping("/{id}/status")
    public ProfessorResponse updateStatus(@PathVariable Long id,
                                          @Valid @RequestBody UpdateStatusRequest request) {
        return professorService.updateStatus(id, request.status());
    }
}
