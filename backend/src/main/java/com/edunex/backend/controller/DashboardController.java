package com.edunex.backend.controller;

import com.edunex.backend.dto.ProfessorDashboard;
import com.edunex.backend.dto.StudentDashboard;
import com.edunex.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/api/student/dashboard")
    @PreAuthorize("hasRole('STUDENT')")
    public StudentDashboard student(Authentication auth) {
        return dashboardService.student(auth.getName());
    }

    @GetMapping("/api/professor/dashboard")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ProfessorDashboard professor(Authentication auth) {
        return dashboardService.professor(auth.getName());
    }
}