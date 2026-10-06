package com.edunex.backend.controller;

import com.edunex.backend.dto.CourseResponse;
import com.edunex.backend.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentCourseController {

    private final CourseService courseService;

    @GetMapping("/subjects")
    public List<CourseResponse.SubjectRef> subjects(Authentication auth) {
        return courseService.studentSubjects(auth.getName());
    }

    @GetMapping("/courses")
    public List<CourseResponse> courses(Authentication auth) {
        return courseService.studentCourses(auth.getName());
    }
}
