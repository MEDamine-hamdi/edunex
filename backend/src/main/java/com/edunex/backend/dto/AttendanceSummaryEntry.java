package com.edunex.backend.dto;

public record AttendanceSummaryEntry(
        Long studentId,
        String studentName,
        String studentNumber,
        long present,
        long absent,
        long late,
        long total
) {
}
