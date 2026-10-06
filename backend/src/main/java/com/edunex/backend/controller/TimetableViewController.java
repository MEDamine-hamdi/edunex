package com.edunex.backend.controller;

import com.edunex.backend.dto.SessionResponse;
import com.edunex.backend.service.TimetableService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TimetableViewController {

    private final TimetableService timetableService;

    @GetMapping("/api/professor/timetable")
    @PreAuthorize("hasRole('PROFESSOR')")
    public List<SessionResponse> professorTimetable(Authentication auth) {
        return timetableService.professorSessions(auth.getName());
    }

    @GetMapping("/api/student/timetable")
    @PreAuthorize("hasRole('STUDENT')")
    public List<SessionResponse> studentTimetable(Authentication auth) {
        return timetableService.studentSessions(auth.getName());
    }
}
