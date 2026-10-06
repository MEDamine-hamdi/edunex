package com.edunex.backend.controller;

import com.edunex.backend.dto.AssignIdsRequest;
import com.edunex.backend.dto.ClassRequest;
import com.edunex.backend.dto.ClassResponse;
import com.edunex.backend.dto.SectionRequest;
import com.edunex.backend.dto.SectionResponse;
import com.edunex.backend.service.SchoolClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/classes")
@PreAuthorize("hasRole('ADMIN_AGENT')")
@RequiredArgsConstructor
public class AdminClassController {

    private final SchoolClassService classService;

    @PostMapping
    public ResponseEntity<ClassResponse> create(@Valid @RequestBody ClassRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(classService.create(request));
    }

    @GetMapping
    public List<ClassResponse> list() {
        return classService.findAll();
    }

    @PutMapping("/{id}")
    public ClassResponse update(@PathVariable Long id, @Valid @RequestBody ClassRequest request) {
        return classService.update(id, request);
    }

    @PutMapping("/{id}/subjects")
    public ClassResponse assignSubjects(@PathVariable Long id,
                                        @Valid @RequestBody AssignIdsRequest request) {
        return classService.assignSubjects(id, request.ids());
    }

    @PostMapping("/{id}/sections")
    public ResponseEntity<SectionResponse> createSection(@PathVariable Long id,
                                                         @Valid @RequestBody SectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(classService.createSection(id, request));
    }

    @GetMapping("/{id}/sections")
    public List<SectionResponse> sections(@PathVariable Long id) {
        return classService.findSections(id);
    }
}
