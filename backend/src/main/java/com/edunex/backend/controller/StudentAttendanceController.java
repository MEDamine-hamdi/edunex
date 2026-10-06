package com.edunex.backend.controller;

import com.edunex.backend.dto.AttendanceResponse;
import com.edunex.backend.dto.StudentAttendanceStats;
import com.edunex.backend.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student/attendance")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentAttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping
    public List<AttendanceResponse> records(@RequestParam(required = false) Long subjectId, Authentication auth) {
        return attendanceService.studentRecords(auth.getName(), subjectId);
    }

    @GetMapping("/stats")
    public List<StudentAttendanceStats> stats(Authentication auth) {
        return attendanceService.studentStats(auth.getName());
    }
}
