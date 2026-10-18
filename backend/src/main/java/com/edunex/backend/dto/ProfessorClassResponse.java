package com.edunex.backend.dto;

import java.util.List;

public record ProfessorClassResponse(
        Long id,
        String name,
        String level,
        String academicYear,
        List<SubjectRef> subjects,
        List<StudentRef> students
) {
    public record SubjectRef(Long id, String name, String code) {
    }

    public record StudentRef(Long id, String name, String studentNumber, String section) {
    }
}