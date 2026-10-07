package com.edunex.backend.dto;

import com.edunex.backend.enums.EvaluationType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record GradeRequest(
        @NotNull Long studentId,
        @NotNull Long subjectId,
        @NotNull @DecimalMin("0.0") @DecimalMax("20.0") Double value,
        @NotNull EvaluationType evaluationType,
        @NotNull LocalDate date
) {
}
