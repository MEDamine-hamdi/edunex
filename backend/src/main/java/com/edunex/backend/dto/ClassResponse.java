package com.edunex.backend.dto;

import java.util.List;

public record ClassResponse(
        Long id,
        String name,
        String level,
        String academicYear,
        List<SubjectRef> subjects
) {
    public record SubjectRef(Long id, String name, String code) {
    }
}
