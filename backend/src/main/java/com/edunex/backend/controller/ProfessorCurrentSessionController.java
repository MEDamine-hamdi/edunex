package com.edunex.backend.controller;

import com.edunex.backend.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/professor/attendance")
@PreAuthorize("hasRole('PROFESSOR') and hasAuthority('ATTENDANCE_MARK')")
@RequiredArgsConstructor
public class ProfessorCurrentSessionController {

    private final AttendanceService attendanceService;

    @GetMapping("/current")
    public AttendanceService.CurrentSessions current(Authentication auth) {
        return attendanceService.currentSessions(auth.getName());
    }
}