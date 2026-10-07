package com.edunex.backend.controller;

import com.edunex.backend.dto.GradeResponse;
import com.edunex.backend.dto.StudentGradesSummary;
import com.edunex.backend.service.GradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student/grades")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentGradeController {

    private final GradeService gradeService;

    @GetMapping
    public List<GradeResponse> grades(@RequestParam(required = false) Long subjectId, Authentication auth) {
        return gradeService.studentGrades(auth.getName(), subjectId);
    }

    @GetMapping("/summary")
    public StudentGradesSummary summary(Authentication auth) {
        return gradeService.studentSummary(auth.getName());
    }
}
