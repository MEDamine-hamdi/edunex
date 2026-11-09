package com.edunex.backend.controller;

import com.edunex.backend.dto.ExamDto;
import com.edunex.backend.service.ExamService;
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
public class ExamStudentController {

    private final ExamService service;

    @GetMapping("/api/student/exams")
    @PreAuthorize("hasRole('STUDENT')")
    public List<ExamDto.StudentExam> mine(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                          Authentication auth) {
        return service.studentExams(auth.getName(), from == null ? LocalDate.now().minusDays(120) : from, to == null ? LocalDate.now().plusDays(365) : to);
    }
}