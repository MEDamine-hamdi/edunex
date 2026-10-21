package com.edunex.backend.service;

import com.edunex.backend.dto.AttendanceResponse;
import com.edunex.backend.dto.AttendanceSummaryEntry;
import com.edunex.backend.dto.MarkAttendanceRequest;
import com.edunex.backend.dto.RosterEntry;
import com.edunex.backend.dto.StudentAttendanceStats;
import com.edunex.backend.entity.Attendance;
import com.edunex.backend.entity.Enrollment;
import com.edunex.backend.entity.Professor;
import com.edunex.backend.entity.SchoolClass;
import com.edunex.backend.entity.Student;
import com.edunex.backend.entity.Subject;
import com.edunex.backend.entity.TimetableSession;
import com.edunex.backend.enums.AttendanceStatus;
import com.edunex.backend.enums.EnrollmentStatus;
import com.edunex.backend.enums.NotificationType;
import com.edunex.backend.exception.BadRequestException;
import com.edunex.backend.exception.ConflictException;
import com.edunex.backend.repository.AttendanceRepository;
import com.edunex.backend.repository.EnrollmentRepository;
import com.edunex.backend.repository.SchoolClassRepository;
import com.edunex.backend.repository.SessionAbsenceRepository;
import com.edunex.backend.repository.SubjectRepository;
import com.edunex.backend.repository.TimetableSessionRepository;
import com.edunex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Attendance can only be taken for the professor's session in progress (today, between the opening and closing
 * of its window) and only for the students of that session's class. A session marked absent is not in progress.
 */
@Service
@RequiredArgsConstructor
public class AttendanceService {

    public record SessionInfo(Long sessionId, Long subjectId, String subjectName, String subjectCode, Long classId,
                              String className, String room, String startTime, String endTime,
                              String opensAt, String closesAt) {
    }

    public record CurrentSessions(String now, List<SessionInfo> open, List<SessionInfo> upcoming) {
    }

    private final AttendanceRepository attendanceRepository;
    private final SubjectRepository subjectRepository;
    private final SchoolClassRepository classRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final StudentAccessService studentAccessService;
    private final NotificationService notificationService;
    private final TimetableSessionRepository sessionRepository;
    private final SessionAbsenceRepository absenceRepository;

    @Value("${app.attendance.early-minutes:10}")
    private int earlyMinutes;

    @Value("${app.attendance.late-minutes:15}")
    private int lateMinutes;

    // ---------- professor ----------

    @Transactional(readOnly = true)
    public CurrentSessions currentSessions(String email) {
        Professor p = professor(email);
        LocalDateTime now = LocalDateTime.now();
        List<SessionInfo> open = new ArrayList<>();
        List<SessionInfo> upcoming = new ArrayList<>();
        for (TimetableSession s : sessionRepository.findByProfessorId(p.getId())) {
            if (s.getDayOfWeek() != now.getDayOfWeek() || absent(s, now.toLocalDate())) {
                continue;
            }
            LocalTime t = now.toLocalTime();
            LocalTime opens = s.getStartTime().minusMinutes(earlyMinutes);
            LocalTime closes = s.getEndTime().plusMinutes(lateMinutes);
            if (!t.isBefore(opens) && !t.isAfter(closes)) {
                open.add(info(s, opens, closes));
            } else if (t.isBefore(opens)) {
                upcoming.add(info(s, opens, closes));
            }
        }
        upcoming.sort(Comparator.comparing(SessionInfo::startTime));
        return new CurrentSessions(now.toString(), open, upcoming);
    }

    @Transactional(readOnly = true)
    public List<RosterEntry> roster(String email, Long subjectId) {
        TimetableSession session = openSession(professor(email), subjectId);
        return classRoster(session.getSchoolClass().getId()).values().stream()
                .sorted(Comparator.comparing((Enrollment e) -> e.getStudent().getLastName())
                        .thenComparing(e -> e.getStudent().getFirstName()))
                .map(e -> new RosterEntry(e.getStudent().getId(),
                        e.getStudent().getFirstName() + " " + e.getStudent().getLastName(),
                        e.getStudent().getStudentNumber(), e.getSchoolClass().getName()))
                .toList();
    }

    /** Today's records of the session in progress (the date parameter is ignored: only today can be edited). */
    @Transactional(readOnly = true)
    public List<AttendanceResponse> forDate(String email, Long subjectId, LocalDate ignored) {
        TimetableSession session = openSession(professor(email), subjectId);
        return recordsOfToday(subjectId, classRoster(session.getSchoolClass().getId()).keySet());
    }

    @Transactional
    public List<AttendanceResponse> mark(String email, Long subjectId, MarkAttendanceRequest req) {
        Professor p = professor(email);
        TimetableSession session = openSession(p, subjectId);
        if (!LocalDate.now().equals(req.date())) {
            throw new BadRequestException("Attendance can only be taken today, during the session");
        }
        Subject subject = session.getSubject();
        Map<Long, Enrollment> roster = classRoster(session.getSchoolClass().getId());
        for (MarkAttendanceRequest.Entry entry : req.records()) {
            Enrollment enrollment = roster.get(entry.studentId());
            if (enrollment == null) {
                throw new BadRequestException("Student " + entry.studentId() + " is not in the class of this session");
            }
            Attendance a = attendanceRepository
                    .findByStudentIdAndSubjectIdAndDate(entry.studentId(), subjectId, req.date())
                    .orElseGet(Attendance::new);
            String justification = entry.justification() == null || entry.justification().isBlank()
                    ? null : entry.justification().trim();
            AttendanceStatus previousStatus = a.getStatus();
            a.setStudent(enrollment.getStudent());
            a.setSubject(subject);
            a.setDate(req.date());
            a.setStatus(entry.status());
            a.setJustification(entry.status() == AttendanceStatus.PRESENT ? null : justification);
            a.setMarkedBy(p);
            attendanceRepository.save(a);
            if (entry.status() != AttendanceStatus.PRESENT && entry.status() != previousStatus) {
                notificationService.notifyUsers(List.of(enrollment.getStudent()), NotificationType.ATTENDANCE_UPDATE,
                        (entry.status() == AttendanceStatus.ABSENT ? "Absence" : "Retard")
                                + " enregistré(e) en " + subject.getName() + " le " + req.date());
            }
        }
        return recordsOfToday(subjectId, roster.keySet());
    }

    @Transactional(readOnly = true)
    public List<AttendanceSummaryEntry> summary(String email, Long subjectId) {
        Professor p = professor(email);
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new com.edunex.backend.exception.ResourceNotFoundException("Subject not found"));
        if (subject.getProfessors().stream().noneMatch(x -> x.getId().equals(p.getId()))) {
            throw new AccessDeniedException("You are not assigned to this subject");
        }
        Map<Long, Student> students = new LinkedHashMap<>();
        List<Long> classIds = classRepository.findBySubjectsId(subjectId).stream().map(SchoolClass::getId).toList();
        if (!classIds.isEmpty()) {
            enrollmentRepository.findBySchoolClassIdInAndStatus(classIds, EnrollmentStatus.ACTIVE)
                    .forEach(e -> students.putIfAbsent(e.getStudent().getId(), e.getStudent()));
        }
        List<Attendance> records = attendanceRepository.findBySubjectId(subjectId);
        records.forEach(a -> students.putIfAbsent(a.getStudent().getId(), a.getStudent()));
        Map<Long, List<Attendance>> byStudent = records.stream()
                .collect(Collectors.groupingBy(a -> a.getStudent().getId()));
        return students.values().stream()
                .sorted(Comparator.comparing(Student::getLastName).thenComparing(Student::getFirstName))
                .map(s -> {
                    List<Attendance> list = byStudent.getOrDefault(s.getId(), List.of());
                    return new AttendanceSummaryEntry(s.getId(), s.getFirstName() + " " + s.getLastName(),
                            s.getStudentNumber(), count(list, AttendanceStatus.PRESENT),
                            count(list, AttendanceStatus.ABSENT), count(list, AttendanceStatus.LATE), list.size());
                })
                .toList();
    }

    // ---------- student (own data only) ----------

    @Transactional(readOnly = true)
    public List<AttendanceResponse> studentRecords(String email, Long subjectId) {
        Student s = student(email);
        List<Attendance> list = subjectId == null
                ? attendanceRepository.findByStudentId(s.getId())
                : attendanceRepository.findByStudentIdAndSubjectId(s.getId(), subjectId);
        return list.stream()
                .sorted(Comparator.comparing(Attendance::getDate).reversed().thenComparing(Attendance::getId))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudentAttendanceStats> studentStats(String email) {
        Student s = student(email);
        List<Attendance> records = attendanceRepository.findByStudentId(s.getId());
        Set<Long> ids = new HashSet<>(studentAccessService.accessibleSubjectIds(s.getId()));
        records.forEach(a -> ids.add(a.getSubject().getId()));
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, List<Attendance>> bySubject = records.stream()
                .collect(Collectors.groupingBy(a -> a.getSubject().getId()));
        return subjectRepository.findAllById(ids).stream()
                .sorted(Comparator.comparing(Subject::getName))
                .map(sub -> {
                    List<Attendance> list = bySubject.getOrDefault(sub.getId(), List.of());
                    long absent = count(list, AttendanceStatus.ABSENT);
                    double rate = list.isEmpty() ? 0 : Math.round(absent * 1000.0 / list.size()) / 10.0;
                    return new StudentAttendanceStats(sub.getId(), sub.getName(), sub.getCode(),
                            count(list, AttendanceStatus.PRESENT), absent, count(list, AttendanceStatus.LATE),
                            list.size(), rate);
                })
                .toList();
    }

    // ---------- helpers ----------

    private TimetableSession openSession(Professor p, Long subjectId) {
        LocalDateTime now = LocalDateTime.now();
        LocalTime t = now.toLocalTime();
        for (TimetableSession s : sessionRepository.findByProfessorId(p.getId())) {
            if (!s.getSubject().getId().equals(subjectId) || s.getDayOfWeek() != now.getDayOfWeek()
                    || absent(s, now.toLocalDate())) {
                continue;
            }
            if (!t.isBefore(s.getStartTime().minusMinutes(earlyMinutes))
                    && !t.isAfter(s.getEndTime().plusMinutes(lateMinutes))) {
                return s;
            }
        }
        throw new ConflictException("No session in progress for this subject");
    }

    private boolean absent(TimetableSession s, LocalDate date) {
        return absenceRepository.existsBySessionIdAndDate(s.getId(), date);
    }

    private SessionInfo info(TimetableSession s, LocalTime opens, LocalTime closes) {
        return new SessionInfo(s.getId(), s.getSubject().getId(), s.getSubject().getName(), s.getSubject().getCode(),
                s.getSchoolClass().getId(), s.getSchoolClass().getName(), s.getRoom(), s.getStartTime().toString(),
                s.getEndTime().toString(), opens.toString(), closes.toString());
    }

    private Map<Long, Enrollment> classRoster(Long classId) {
        Map<Long, Enrollment> map = new LinkedHashMap<>();
        for (Enrollment e : enrollmentRepository.findBySchoolClassIdInAndStatus(List.of(classId), EnrollmentStatus.ACTIVE)) {
            map.putIfAbsent(e.getStudent().getId(), e);
        }
        return map;
    }

    private List<AttendanceResponse> recordsOfToday(Long subjectId, Set<Long> studentIds) {
        return attendanceRepository.findBySubjectIdAndDate(subjectId, LocalDate.now()).stream()
                .filter(a -> studentIds.contains(a.getStudent().getId()))
                .sorted(Comparator.comparing((Attendance a) -> a.getStudent().getLastName())
                        .thenComparing(a -> a.getStudent().getFirstName()))
                .map(this::toResponse)
                .toList();
    }

    private Professor professor(String email) {
        return userRepository.findByEmail(email).filter(u -> u instanceof Professor).map(u -> (Professor) u)
                .orElseThrow(() -> new AccessDeniedException("Professor account required"));
    }

    private Student student(String email) {
        return userRepository.findByEmail(email).filter(u -> u instanceof Student).map(u -> (Student) u)
                .orElseThrow(() -> new AccessDeniedException("Student account required"));
    }

    private static long count(List<Attendance> list, AttendanceStatus status) {
        return list.stream().filter(a -> a.getStatus() == status).count();
    }

    private AttendanceResponse toResponse(Attendance a) {
        Student s = a.getStudent();
        Subject sub = a.getSubject();
        return new AttendanceResponse(a.getId(), s.getId(), s.getFirstName() + " " + s.getLastName(),
                s.getStudentNumber(), sub.getId(), sub.getName(), a.getDate(), a.getStatus().name(),
                a.getJustification());
    }
}