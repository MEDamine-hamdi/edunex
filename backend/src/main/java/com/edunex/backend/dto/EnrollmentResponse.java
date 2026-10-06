package com.edunex.backend.dto;

import java.time.LocalDate;

public record EnrollmentResponse(
        Long id,
        Long studentId,
        String studentName,
        String studentNumber,
        Long classId,
        String className,
        Long sectionId,
        String sectionName,
        String academicYear,
        LocalDate enrollmentDate,
        String status
) {
}
