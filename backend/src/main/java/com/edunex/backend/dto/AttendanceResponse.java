package com.edunex.backend.dto;

import java.time.LocalDate;

public record AttendanceResponse(
        Long id,
        Long studentId,
        String studentName,
        String studentNumber,
        Long subjectId,
        String subjectName,
        LocalDate date,
        String status,
        String justification
) {
}
