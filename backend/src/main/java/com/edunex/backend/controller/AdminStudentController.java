package com.edunex.backend.controller;

import com.edunex.backend.dto.CreateStudentRequest;
import com.edunex.backend.dto.StudentResponse;
import com.edunex.backend.dto.UpdateStatusRequest;
import com.edunex.backend.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/students")
@PreAuthorize("hasRole('ADMIN_AGENT') and hasAuthority('ACCOUNTS_MANAGE')")
@RequiredArgsConstructor
public class AdminStudentController {

    private final StudentService studentService;

    @PostMapping
    public ResponseEntity<StudentResponse> create(@Valid @RequestBody CreateStudentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentService.create(request));
    }

    @GetMapping
    public List<StudentResponse> list() {
        return studentService.findAll();
    }

    @PatchMapping("/{id}/status")
    public StudentResponse updateStatus(@PathVariable Long id,
                                        @Valid @RequestBody UpdateStatusRequest request) {
        return studentService.updateStatus(id, request.status());
    }
}
