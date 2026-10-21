package com.edunex.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class AbsenceDto {

    private AbsenceDto() {
    }

    public record Info(Long id, String kind, String reason, String declaredBy, LocalDateTime createdAt) {
    }

    /** One timetable session on a given date, with its absence if any. */
    public record WeekSession(Long sessionId, LocalDate date, String dayOfWeek, String startTime, String endTime,
                              String room, Long classId, String className, Long subjectId, String subjectName,
                              String subjectCode, Long professorId, String professorName, Info absence,
                              boolean canDeclare) {
    }

    public record Declare(@NotNull Long sessionId, @NotNull LocalDate date, @Size(max = 500) String reason) {
    }
}