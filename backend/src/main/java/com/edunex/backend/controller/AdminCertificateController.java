package com.edunex.backend.controller;

import com.edunex.backend.dto.CertificateDto;
import com.edunex.backend.service.CertificateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

/** Verification of the certificates and scheduling of the make-up exams. */
@RestController
@PreAuthorize("hasRole('ADMIN_AGENT') and hasAuthority('CERTIFICATES_MANAGE')")
@RequiredArgsConstructor
public class AdminCertificateController {

    private final CertificateService service;

    @GetMapping("/api/admin/certificates")
    public List<CertificateDto.Row> list(@RequestParam(required = false) String status) {
        return service.list(status);
    }

    @GetMapping("/api/admin/certificates/students")
    public List<CertificateDto.StudentHit> students(@RequestParam(required = false) String q) {
        return service.searchStudents(q);
    }

    @GetMapping("/api/admin/certificates/coverage")
    public CertificateDto.Coverage coverage(@RequestParam Long studentId,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.coverageForStudent(studentId, from, to);
    }

    @GetMapping("/api/admin/certificates/{id}")
    public CertificateDto.Detail detail(@PathVariable Long id) {
        return service.detail(id);
    }

    @GetMapping("/api/admin/certificates/{id}/file")
    public ResponseEntity<Resource> file(@PathVariable Long id) {
        return StudentCertificateController.respond(service.adminFile(id));
    }

    @PostMapping("/api/admin/certificates/{id}/approve")
    public CertificateDto.Row approve(@PathVariable Long id, @Valid @RequestBody CertificateDto.DecisionRequest request, Authentication auth) {
        return service.approve(id, auth.getName(), request.note());
    }

    @PostMapping("/api/admin/certificates/{id}/reject")
    public CertificateDto.Row reject(@PathVariable Long id, @Valid @RequestBody CertificateDto.RejectRequest request, Authentication auth) {
        return service.reject(id, auth.getName(), request.reason());
    }

    @PostMapping("/api/admin/certificates/{id}/revoke")
    public CertificateDto.Row revoke(@PathVariable Long id, @Valid @RequestBody CertificateDto.DecisionRequest request, Authentication auth) {
        return service.revoke(id, auth.getName(), request.note());
    }

    @PostMapping(value = "/api/admin/certificates/in-person", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CertificateDto.Row> inPerson(
            @RequestParam Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String note,
            @RequestParam(value = "examIds", required = false) List<Long> examIds,
            @RequestParam(value = "file", required = false) MultipartFile file, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.inPerson(auth.getName(), studentId, startDate, endDate, note, examIds, file));
    }

    @GetMapping("/api/admin/retakes")
    public List<CertificateDto.RetakeItem> retakes() {
        return service.allRetakes();
    }

    @PostMapping("/api/admin/retakes/schedule")
    public List<CertificateDto.RetakeItem> schedule(@Valid @RequestBody CertificateDto.ScheduleRequest request, Authentication auth) {
        return service.schedule(request, auth.getName());
    }

    @PostMapping("/api/admin/retakes/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        service.cancelRetake(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/admin/retakes/{id}/done")
    public ResponseEntity<Void> done(@PathVariable Long id) {
        service.markRetakeDone(id);
        return ResponseEntity.noContent().build();
    }
}