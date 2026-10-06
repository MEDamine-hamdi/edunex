package com.edunex.backend.controller;

import com.edunex.backend.dto.CreateEnrollmentRequest;
import com.edunex.backend.dto.EnrollmentResponse;
import com.edunex.backend.dto.UpdateEnrollmentStatusRequest;
import com.edunex.backend.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/enrollments")
@PreAuthorize("hasRole('ADMIN_AGENT')")
@RequiredArgsConstructor
public class AdminEnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    public ResponseEntity<EnrollmentResponse> create(@Valid @RequestBody CreateEnrollmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollmentService.create(request));
    }

    @GetMapping
    public List<EnrollmentResponse> list(@RequestParam(required = false) Long classId,
                                         @RequestParam(required = false) Long studentId) {
        return enrollmentService.findAll(classId, studentId);
    }

    @PatchMapping("/{id}/status")
    public EnrollmentResponse updateStatus(@PathVariable Long id,
                                           @Valid @RequestBody UpdateEnrollmentStatusRequest request) {
        return enrollmentService.updateStatus(id, request.status());
    }
}
