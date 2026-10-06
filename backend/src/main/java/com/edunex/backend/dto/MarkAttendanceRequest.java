package com.edunex.backend.dto;

import com.edunex.backend.enums.AttendanceStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record MarkAttendanceRequest(
        @NotNull LocalDate date,
        @NotEmpty @Valid List<Entry> records
) {
    public record Entry(
            @NotNull Long studentId,
            @NotNull AttendanceStatus status,
            @Size(max = 500) String justification
    ) {
    }
}
