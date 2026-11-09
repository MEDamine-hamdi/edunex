package com.edunex.backend.controller;

import com.edunex.backend.service.EliminationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class EliminationController {

    public record PolicyRequest(Integer maxAbsences, Integer latesPerAbsence) {
    }

    public record WaiveRequest(String reason) {
    }

    private static final String ADMIN = "hasRole('ADMIN_AGENT') and hasAuthority('ELIMINATION_MANAGE')";

    private final EliminationService service;

    @GetMapping("/api/admin/elimination/policy")
    @PreAuthorize(ADMIN)
    public EliminationService.PolicyView policy() {
        return service.policy();
    }

    @PutMapping("/api/admin/elimination/policy/default")
    @PreAuthorize(ADMIN)
    public EliminationService.PolicyView setDefault(@RequestBody PolicyRequest request) {
        return service.setPolicy(null, request.maxAbsences(), request.latesPerAbsence());
    }

    @PutMapping("/api/admin/elimination/policy/subjects/{id}")
    @PreAuthorize(ADMIN)
    public EliminationService.PolicyView setSubject(@PathVariable Long id, @RequestBody PolicyRequest request) {
        return service.setPolicy(id, request.maxAbsences(), request.latesPerAbsence());
    }

    @DeleteMapping("/api/admin/elimination/policy/subjects/{id}")
    @PreAuthorize(ADMIN)
    public EliminationService.PolicyView removeOverride(@PathVariable Long id) {
        return service.removeOverride(id);
    }

    @GetMapping("/api/admin/elimination")
    @PreAuthorize(ADMIN)
    public List<EliminationService.Row> list() {
        return service.list();
    }

    @PostMapping("/api/admin/elimination/{id}/waive")
    @PreAuthorize(ADMIN)
    public ResponseEntity<Void> waive(@PathVariable Long id, @RequestBody WaiveRequest request, Authentication auth) {
        service.waive(id, auth.getName(), request.reason());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/admin/elimination/{id}/restore")
    @PreAuthorize(ADMIN)
    public ResponseEntity<Void> restore(@PathVariable Long id) {
        service.restore(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/student/elimination")
    @PreAuthorize("hasRole('STUDENT')")
    public List<EliminationService.StudentRow> mine(Authentication auth) {
        return service.forStudent(auth.getName());
    }
}