package com.edunex.backend.dto;

public record SessionResponse(
        Long id,
        Long timetableId,
        String academicYear,
        String semester,
        String dayOfWeek,
        String startTime,
        String endTime,
        String room,
        Long classId,
        String className,
        Long subjectId,
        String subjectName,
        String subjectCode,
        Long professorId,
        String professorName
) {
}
