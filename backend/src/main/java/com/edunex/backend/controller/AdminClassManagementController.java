package com.edunex.backend.controller;

import com.edunex.backend.dto.AssignIdsRequest;
import com.edunex.backend.dto.ClassMgmtDto;
import com.edunex.backend.service.ClassManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/class-management")
@PreAuthorize("hasRole('ADMIN_AGENT') and hasAuthority('STRUCTURE_MANAGE')")
@RequiredArgsConstructor
public class AdminClassManagementController {

    private final ClassManagementService service;

    @GetMapping("/professors")
    public List<ClassMgmtDto.ProfRef> professors() {
        return service.professorOptions();
    }

    @GetMapping("/{classId}/overview")
    public ClassMgmtDto.Overview overview(@PathVariable Long classId) {
        return service.overview(classId);
    }

    @GetMapping("/{classId}/candidates")
    public List<ClassMgmtDto.Candidate> candidates(@PathVariable Long classId, @RequestParam(required = false) String q) {
        return service.candidates(classId, q);
    }

    @PutMapping("/{classId}/subjects/{subjectId}/professors")
    public ClassMgmtDto.Overview setProfessors(@PathVariable Long classId, @PathVariable Long subjectId,
                                               @Valid @RequestBody AssignIdsRequest request) {
        return service.setProfessors(classId, subjectId, request.ids());
    }

    @PostMapping("/{classId}/students")
    public ClassMgmtDto.AddResult addStudents(@PathVariable Long classId, @Valid @RequestBody ClassMgmtDto.AddStudents request) {
        return service.addStudents(classId, request);
    }

    @DeleteMapping("/{classId}/students/{studentId}")
    public ResponseEntity<Void> removeStudent(@PathVariable Long classId, @PathVariable Long studentId) {
        service.removeStudent(classId, studentId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{classId}/students/{studentId}/group")
    public ResponseEntity<Void> changeGroup(@PathVariable Long classId, @PathVariable Long studentId,
                                            @RequestBody ClassMgmtDto.GroupChange request) {
        service.changeGroup(classId, studentId, request.groupId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{classId}/groups/{groupId}")
    public ResponseEntity<Void> renameGroup(@PathVariable Long classId, @PathVariable Long groupId,
                                            @Valid @RequestBody ClassMgmtDto.GroupName request) {
        service.renameGroup(classId, groupId, request.name());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{classId}/groups/{groupId}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long classId, @PathVariable Long groupId) {
        service.deleteGroup(classId, groupId);
        return ResponseEntity.noContent().build();
    }
}