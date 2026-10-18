package com.edunex.backend.dto;

import java.util.List;

public record PlatformStats(
        long totalUsers,
        List<RoleCount> usersByRole,
        List<StatusCount> usersByStatus,
        long logEntries,
        long settings,
        List<DayActivity> last7Days
) {
    public record RoleCount(String role, long count) {
    }

    public record StatusCount(String status, long count) {
    }

    public record DayActivity(String date, long logins, long failedLogins, long changes) {
    }
}