package com.edunex.backend.dto;

import java.util.List;

public record StudentGradesSummary(List<SubjectAverage> subjects, Double overallAverage) {

    public record SubjectAverage(
            Long subjectId,
            String subjectName,
            String subjectCode,
            Double coefficient,
            Double average,
            int gradeCount
    ) {
    }
}
