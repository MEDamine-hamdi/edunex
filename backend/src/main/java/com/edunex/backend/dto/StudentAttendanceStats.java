package com.edunex.backend.dto;

public record StudentAttendanceStats(
        Long subjectId,
        String subjectName,
        String subjectCode,
        long present,
        long absent,
        long late,
        long total,
        double absenceRate
) {
}
