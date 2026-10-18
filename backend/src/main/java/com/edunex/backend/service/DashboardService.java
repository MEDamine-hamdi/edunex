package com.edunex.backend.service;

import com.edunex.backend.dto.CourseResponse;
import com.edunex.backend.dto.ProfessorClassResponse;
import com.edunex.backend.dto.ProfessorDashboard;
import com.edunex.backend.dto.SessionResponse;
import com.edunex.backend.dto.StudentAttendanceStats;
import com.edunex.backend.dto.StudentDashboard;
import com.edunex.backend.dto.StudentGradesSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

/** Composes the existing services, so every access rule still applies. */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final GradeService gradeService;
    private final AttendanceService attendanceService;
    private final TimetableService timetableService;
    private final CourseService courseService;
    private final NotificationService notificationService;
    private final ProfessorClassService professorClassService;

    public StudentDashboard student(String email) {
        StudentGradesSummary grades = gradeService.studentSummary(email);
        List<StudentAttendanceStats> attendance = attendanceService.studentStats(email);
        List<CourseResponse> courses = courseService.studentCourses(email);
        return new StudentDashboard(
                grades.subjects().size(),
                grades.overallAverage(),
                attendance.stream().mapToLong(StudentAttendanceStats::absent).sum(),
                attendance.stream().mapToLong(StudentAttendanceStats::late).sum(),
                notificationService.unreadCount(email),
                upcoming(timetableService.studentSessions(email)),
                courses.stream().limit(3)
                        .map(c -> new StudentDashboard.CourseBrief(c.id(), c.title(), c.subject().name(), c.createdAt()))
                        .toList());
    }

    public ProfessorDashboard professor(String email) {
        List<CourseResponse> courses = courseService.listMyCourses(email);
        List<ProfessorClassResponse> classes = professorClassService.myClasses(email);
        long students = classes.stream()
                .flatMap(c -> c.students().stream())
                .map(ProfessorClassResponse.StudentRef::id)
                .distinct()
                .count();
        return new ProfessorDashboard(
                courseService.professorSubjects(email).size(),
                courses.size(),
                courses.stream().mapToLong(c -> c.documents().size()).sum(),
                classes.size(),
                students,
                notificationService.unreadCount(email),
                upcoming(timetableService.professorSessions(email)));
    }

    private List<SessionResponse> upcoming(List<SessionResponse> sessions) {
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek today = now.getDayOfWeek();
        LocalTime time = now.toLocalTime();
        return sessions.stream()
                .sorted(Comparator.comparingLong((SessionResponse s) -> minutesUntil(s, today, time)))
                .limit(3)
                .toList();
    }

    private static long minutesUntil(SessionResponse s, DayOfWeek today, LocalTime now) {
        int days = (DayOfWeek.valueOf(s.dayOfWeek()).getValue() - today.getValue() + 7) % 7;
        if (days == 0 && LocalTime.parse(s.endTime()).isBefore(now)) {
            days = 7;
        }
        return days * 24L * 60 + LocalTime.parse(s.startTime()).toSecondOfDay() / 60;
    }
}