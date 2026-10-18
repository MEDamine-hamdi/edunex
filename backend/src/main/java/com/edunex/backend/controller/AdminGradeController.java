package com.edunex.backend.controller;

import com.edunex.backend.dto.GradeRequest;
import com.edunex.backend.dto.GradeResponse;
import com.edunex.backend.dto.GradeUpdateRequest;
import com.edunex.backend.dto.RosterEntry;
import com.edunex.backend.service.GradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN_AGENT') and hasAuthority('GRADES_MANAGE')")
@RequiredArgsConstructor
public class AdminGradeController {

    private final GradeService gradeService;

    @GetMapping("/subjects/{subjectId}/students")
    public List<RosterEntry> subjectStudents(@PathVariable Long subjectId) {
        return gradeService.subjectStudents(subjectId);
    }

    @GetMapping("/grades")
    public List<GradeResponse> list(@RequestParam(required = false) Long subjectId,
                                    @RequestParam(required = false) Long studentId) {
        return gradeService.list(subjectId, studentId);
    }

    @PostMapping("/grades")
    public ResponseEntity<GradeResponse> create(@Valid @RequestBody GradeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gradeService.create(request));
    }

    @PutMapping("/grades/{id}")
    public GradeResponse update(@PathVariable Long id, @Valid @RequestBody GradeUpdateRequest request) {
        return gradeService.update(id, request);
    }

    @DeleteMapping("/grades/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        gradeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
