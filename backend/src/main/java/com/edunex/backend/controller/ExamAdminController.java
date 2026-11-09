package com.edunex.backend.controller;

import com.edunex.backend.dto.ExamDto;
import com.edunex.backend.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/exams")
@PreAuthorize("hasRole('ADMIN_AGENT') and hasAuthority('EXAMS_MANAGE')")
@RequiredArgsConstructor
public class ExamAdminController {

    private final ExamService service;

    @GetMapping("/lookups")
    public ExamDto.Lookups lookups() {
        return service.lookups();
    }

    @GetMapping
    public List<ExamDto.ExamItem> list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.adminList(from == null ? LocalDate.now().minusDays(60) : from, to == null ? LocalDate.now().plusDays(365) : to);
    }

    @PostMapping
    public ResponseEntity<ExamDto.ExamItem> create(@Valid @RequestBody ExamDto.SaveExam request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public ExamDto.ExamItem update(@PathVariable Long id, @Valid @RequestBody ExamDto.SaveExam request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/cancel")
    public ExamDto.ExamItem cancel(@PathVariable Long id, @Valid @RequestBody ExamDto.ReasonRequest request) {
        return service.cancel(id, request.reason());
    }

    @PostMapping("/{id}/supervisors")
    public ExamDto.AssignResult assign(@PathVariable Long id, @Valid @RequestBody ExamDto.AssignRequest request, Authentication auth) {
        return service.assign(id, request.professorId(), auth.getName());
    }

    @DeleteMapping("/{id}/supervisors/{professorId}")
    public ExamDto.ExamItem remove(@PathVariable Long id, @PathVariable Long professorId,
                                   @RequestParam(required = false) String reason, Authentication auth) {
        return service.remove(id, professorId, auth.getName(), reason);
    }

    @GetMapping("/supervisors")
    public List<ExamDto.ProfessorRow> professors() {
        return service.professorOverview();
    }

    @PutMapping("/supervisors/{professorId}/kind")
    public ResponseEntity<Void> setKind(@PathVariable Long professorId, @Valid @RequestBody ExamDto.KindRequest request) {
        service.setKind(professorId, request.kind());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/history")
    public List<ExamDto.EventRow> history(@RequestParam(required = false) Long professorId) {
        return service.history(professorId);
    }
}