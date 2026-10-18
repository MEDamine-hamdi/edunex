package com.edunex.backend.dto;

import java.util.List;

public record UniversityStats(
        Counts counts,
        List<ClassCount> enrollmentsByClass,
        AttendanceStats attendance,
        GradeStats grades
) {
    public record Counts(long students, long activeStudents, long professors, long classes, long subjects,
                         long activeEnrollments, long courses, long documents, long sessions) {
    }

    public record ClassCount(String className, long count) {
    }

    public record AttendanceStats(long present, long absent, long late, long total, double absenceRate,
                                  List<SubjectAbsence> bySubject) {
    }

    public record SubjectAbsence(String subjectName, long absent, long total, double absenceRate) {
    }

    public record GradeStats(long count, Double average, List<Bucket> distribution, List<SubjectAverage> bySubject) {
    }

    public record Bucket(String label, long count) {
    }

    public record SubjectAverage(String subjectName, Double average, long count) {
    }
}