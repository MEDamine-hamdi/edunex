package com.edunex.backend.controller;

import com.edunex.backend.dto.AttendanceResponse;
import com.edunex.backend.dto.AttendanceSummaryEntry;
import com.edunex.backend.dto.MarkAttendanceRequest;
import com.edunex.backend.dto.RosterEntry;
import com.edunex.backend.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/professor/subjects/{subjectId}")
@PreAuthorize("hasRole('PROFESSOR') and hasAuthority('ATTENDANCE_MARK')")
@RequiredArgsConstructor
public class ProfessorAttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/students")
    public List<RosterEntry> roster(@PathVariable Long subjectId, Authentication auth) {
        return attendanceService.roster(auth.getName(), subjectId);
    }

    @GetMapping("/attendance")
    public List<AttendanceResponse> forDate(@PathVariable Long subjectId,
                                            @RequestParam(required = false)
                                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                            Authentication auth) {
        return attendanceService.forDate(auth.getName(), subjectId, date);
    }

    @PutMapping("/attendance")
    public List<AttendanceResponse> mark(@PathVariable Long subjectId,
                                         @Valid @RequestBody MarkAttendanceRequest request,
                                         Authentication auth) {
        return attendanceService.mark(auth.getName(), subjectId, request);
    }

    @GetMapping("/attendance/summary")
    public List<AttendanceSummaryEntry> summary(@PathVariable Long subjectId, Authentication auth) {
        return attendanceService.summary(auth.getName(), subjectId);
    }
}
