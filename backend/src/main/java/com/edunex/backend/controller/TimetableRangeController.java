package com.edunex.backend.controller;

import com.edunex.backend.dto.AbsenceDto;
import com.edunex.backend.service.TeacherAbsenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class TimetableRangeController {

    private final TeacherAbsenceService service;

    @GetMapping("/api/student/timetable/range")
    @PreAuthorize("hasRole('STUDENT')")
    public List<AbsenceDto.WeekSession> student(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to, Authentication auth) {
        return service.studentRange(auth.getName(), from, to);
    }

    @GetMapping("/api/professor/timetable/range")
    @PreAuthorize("hasRole('PROFESSOR')")
    public List<AbsenceDto.WeekSession> professor(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to, Authentication auth) {
        return service.professorRange(auth.getName(), from, to);
    }

    @GetMapping("/api/admin/timetable/range")
    @PreAuthorize("hasRole('ADMIN_AGENT') and hasAuthority('TIMETABLE_MANAGE')")
    public List<AbsenceDto.WeekSession> admin(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long classId, @RequestParam(required = false) Long professorId) {
        return service.adminRange(from, to, classId, professorId);
    }
}