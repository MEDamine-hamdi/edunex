package com.edunex.backend.controller;

import com.edunex.backend.dto.AssignIdsRequest;
import com.edunex.backend.dto.SubjectRequest;
import com.edunex.backend.dto.SubjectResponse;
import com.edunex.backend.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/subjects")
@PreAuthorize("hasRole('ADMIN_AGENT')")
@RequiredArgsConstructor
public class AdminSubjectController {

    private final SubjectService subjectService;

    @PostMapping
    public ResponseEntity<SubjectResponse> create(@Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subjectService.create(request));
    }

    @GetMapping
    public List<SubjectResponse> list() {
        return subjectService.findAll();
    }

    @PutMapping("/{id}")
    public SubjectResponse update(@PathVariable Long id, @Valid @RequestBody SubjectRequest request) {
        return subjectService.update(id, request);
    }

    @PutMapping("/{id}/professors")
    public SubjectResponse assignProfessors(@PathVariable Long id,
                                            @Valid @RequestBody AssignIdsRequest request) {
        return subjectService.assignProfessors(id, request.ids());
    }
}
