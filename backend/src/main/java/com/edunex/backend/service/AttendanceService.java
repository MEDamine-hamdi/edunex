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
import com.edunex.backend.enums.AttendanceStatus;
import com.edunex.backend.enums.EnrollmentStatus;
import com.edunex.backend.enums.NotificationType;
import com.edunex.backend.exception.BadRequestException;
import com.edunex.backend.exception.ResourceNotFoundException;
import com.edunex.backend.repository.AttendanceRepository;
import com.edunex.backend.repository.EnrollmentRepository;
import com.edunex.backend.repository.SchoolClassRepository;
import com.edunex.backend.repository.SubjectRepository;
import com.edunex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final SubjectRepository subjectRepository;
    private final SchoolClassRepository classRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final StudentAccessService studentAccessService;
    private final NotificationService notificationService;

    // ---------- professor ----------

    @Transactional(readOnly = true)
    public List<RosterEntry> roster(String email, Long subjectId) {
        Professor p = professor(email);
        ownSubject(p, subjectId);
        return rosterOf(subjectId).values().stream()
                .sorted(Comparator.comparing((Enrollment e) -> e.getStudent().getLastName())
                        .thenComparing(e -> e.getStudent().getFirstName()))
                .map(e -> new RosterEntry(
                        e.getStudent().getId(),
                        e.getStudent().getFirstName() + " " + e.getStudent().getLastName(),
                        e.getStudent().getStudentNumber(),
                        e.getSchoolClass().getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> forDate(String email, Long subjectId, LocalDate date) {
        Professor p = professor(email);
        ownSubject(p, subjectId);
        return recordsForDate(subjectId, date != null ? date : LocalDate.now());
    }

    @Transactional
    public List<AttendanceResponse> mark(String email, Long subjectId, MarkAttendanceRequest req) {
        Professor p = professor(email);
        Subject subject = ownSubject(p, subjectId);
        if (req.date().isAfter(LocalDate.now())) {
            throw new BadRequestException("Date cannot be in the future");
        }

        Map<Long, Enrollment> roster = rosterOf(subjectId);
        for (MarkAttendanceRequest.Entry entry : req.records()) {
            Enrollment enrollment = roster.get(entry.studentId());
            if (enrollment == null) {
                throw new BadRequestException("Student " + entry.studentId() + " is not enrolled in this subject");
            }
            Attendance a = attendanceRepository
                    .findByStudentIdAndSubjectIdAndDate(entry.studentId(), subjectId, req.date())
                    .orElseGet(Attendance::new);
            String justification = entry.justification() == null || entry.justification().isBlank()
                    ? null : entry.justification().trim();
            a.setStudent(enrollment.getStudent());
            a.setSubject(subject);
            a.setDate(req.date());
            AttendanceStatus previousStatus = a.getStatus();
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
        return recordsForDate(subjectId, req.date());
    }

    @Transactional(readOnly = true)
    public List<AttendanceSummaryEntry> summary(String email, Long subjectId) {
        Professor p = professor(email);
        ownSubject(p, subjectId);

        Map<Long, Student> students = new LinkedHashMap<>();
        rosterOf(subjectId).values().forEach(e -> students.put(e.getStudent().getId(), e.getStudent()));
        List<Attendance> records = attendanceRepository.findBySubjectId(subjectId);
        records.forEach(a -> students.putIfAbsent(a.getStudent().getId(), a.getStudent()));
        Map<Long, List<Attendance>> byStudent = records.stream()
                .collect(Collectors.groupingBy(a -> a.getStudent().getId()));

        return students.values().stream()
                .sorted(Comparator.comparing(Student::getLastName).thenComparing(Student::getFirstName))
                .map(s -> {
                    List<Attendance> list = byStudent.getOrDefault(s.getId(), List.of());
                    return new AttendanceSummaryEntry(
                            s.getId(), s.getFirstName() + " " + s.getLastName(), s.getStudentNumber(),
                            count(list, AttendanceStatus.PRESENT),
                            count(list, AttendanceStatus.ABSENT),
                            count(list, AttendanceStatus.LATE),
                            list.size());
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
                    return new StudentAttendanceStats(
                            sub.getId(), sub.getName(), sub.getCode(),
                            count(list, AttendanceStatus.PRESENT), absent,
                            count(list, AttendanceStatus.LATE), list.size(), rate);
                })
                .toList();
    }

    // ---------- helpers ----------

    private Map<Long, Enrollment> rosterOf(Long subjectId) {
        List<Long> classIds = classRepository.findBySubjectsId(subjectId).stream()
                .map(SchoolClass::getId)
                .toList();
        Map<Long, Enrollment> map = new LinkedHashMap<>();
        if (classIds.isEmpty()) {
            return map;
        }
        for (Enrollment e : enrollmentRepository.findBySchoolClassIdInAndStatus(classIds, EnrollmentStatus.ACTIVE)) {
            map.putIfAbsent(e.getStudent().getId(), e);
        }
        return map;
    }

    private List<AttendanceResponse> recordsForDate(Long subjectId, LocalDate date) {
        return attendanceRepository.findBySubjectIdAndDate(subjectId, date).stream()
                .sorted(Comparator.comparing((Attendance a) -> a.getStudent().getLastName())
                        .thenComparing(a -> a.getStudent().getFirstName()))
                .map(this::toResponse)
                .toList();
    }

    private Subject ownSubject(Professor p, Long subjectId) {
        Subject s = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        boolean teaches = s.getProfessors().stream().anyMatch(x -> x.getId().equals(p.getId()));
        if (!teaches) {
            throw new AccessDeniedException("You are not assigned to this subject");
        }
        return s;
    }

    private Professor professor(String email) {
        return userRepository.findByEmail(email)
                .filter(u -> u instanceof Professor)
                .map(u -> (Professor) u)
                .orElseThrow(() -> new AccessDeniedException("Professor account required"));
    }

    private Student student(String email) {
        return userRepository.findByEmail(email)
                .filter(u -> u instanceof Student)
                .map(u -> (Student) u)
                .orElseThrow(() -> new AccessDeniedException("Student account required"));
    }

    private static long count(List<Attendance> list, AttendanceStatus status) {
        return list.stream().filter(a -> a.getStatus() == status).count();
    }

    private AttendanceResponse toResponse(Attendance a) {
        Student s = a.getStudent();
        Subject sub = a.getSubject();
        return new AttendanceResponse(
                a.getId(), s.getId(), s.getFirstName() + " " + s.getLastName(), s.getStudentNumber(),
                sub.getId(), sub.getName(), a.getDate(), a.getStatus().name(), a.getJustification());
    }
}
