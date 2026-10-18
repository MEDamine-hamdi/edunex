package com.edunex.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record StudentDashboard(
        int subjects,
        Double overallAverage,
        long absences,
        long lates,
        long unreadNotifications,
        List<SessionResponse> upcomingSessions,
        List<CourseBrief> latestCourses
) {
    public record CourseBrief(Long id, String title, String subjectName, LocalDateTime createdAt) {
    }
}