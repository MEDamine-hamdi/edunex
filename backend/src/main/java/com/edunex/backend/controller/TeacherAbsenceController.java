package com.edunex.backend.controller;

import com.edunex.backend.dto.AbsenceDto;
import com.edunex.backend.service.TeacherAbsenceService;
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
@RequiredArgsConstructor
public class TeacherAbsenceController {

    private static final String ADMIN = "hasRole('ADMIN_AGENT') and hasAuthority('TIMETABLE_MANAGE')";

    private final TeacherAbsenceService service;

    @GetMapping("/api/student/timetable/week")
    @PreAuthorize("hasRole('STUDENT')")
    public List<AbsenceDto.WeekSession> studentWeek(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication auth) {
        return service.studentWeek(auth.getName(), date);
    }

    @GetMapping("/api/professor/timetable/week")
    @PreAuthorize("hasRole('PROFESSOR')")
    public List<AbsenceDto.WeekSession> professorWeek(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication auth) {
        return service.professorWeek(auth.getName(), date);
    }

    @GetMapping("/api/admin/timetable/week")
    @PreAuthorize(ADMIN)
    public List<AbsenceDto.WeekSession> adminWeek(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long professorId) {
        return service.adminWeek(date, classId, professorId);
    }

    @PostMapping("/api/professor/absences")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<AbsenceDto.WeekSession> declare(@Valid @RequestBody AbsenceDto.Declare request,
                                                          Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.declare(auth.getName(), request.sessionId(), request.date(), request.reason()));
    }

    @DeleteMapping("/api/professor/absences/{id}")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<Void> cancelOwn(@PathVariable Long id, Authentication auth) {
        service.cancelOwn(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/admin/absences")
    @PreAuthorize(ADMIN)
    public ResponseEntity<AbsenceDto.WeekSession> mark(@Valid @RequestBody AbsenceDto.Declare request,
                                                       Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.markByAdmin(auth.getName(), request.sessionId(), request.date(), request.reason()));
    }

    @DeleteMapping("/api/admin/absences/{id}")
    @PreAuthorize(ADMIN)
    public ResponseEntity<Void> cancelByAdmin(@PathVariable Long id) {
        service.cancelByAdmin(id);
        return ResponseEntity.noContent().build();
    }
}