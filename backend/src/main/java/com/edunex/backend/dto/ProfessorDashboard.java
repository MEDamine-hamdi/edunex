package com.edunex.backend.dto;

import java.util.List;

public record ProfessorDashboard(
        int subjects,
        int courses,
        long documents,
        int classes,
        long students,
        long unreadNotifications,
        List<SessionResponse> upcomingSessions
) {
}