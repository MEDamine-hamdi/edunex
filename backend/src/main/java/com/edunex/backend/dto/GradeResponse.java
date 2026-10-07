package com.edunex.backend.dto;

import java.time.LocalDate;

public record GradeResponse(
        Long id,
        Long studentId,
        String studentName,
        String studentNumber,
        Long subjectId,
        String subjectName,
        String subjectCode,
        Double value,
        String evaluationType,
        LocalDate date
) {
}
