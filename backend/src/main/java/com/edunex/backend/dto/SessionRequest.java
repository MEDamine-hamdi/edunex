package com.edunex.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record SessionRequest(
        @NotNull DayOfWeek dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        String room,
        @NotNull Long classId,
        @NotNull Long subjectId,
        @NotNull Long professorId
) {
}
