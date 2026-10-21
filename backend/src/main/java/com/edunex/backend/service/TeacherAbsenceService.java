package com.edunex.backend.service;

import com.edunex.backend.dto.AbsenceDto.Info;
import com.edunex.backend.dto.AbsenceDto.WeekSession;
import com.edunex.backend.entity.Professor;
import com.edunex.backend.entity.SessionAbsence;
import com.edunex.backend.entity.Student;
import com.edunex.backend.entity.TimetableSession;
import com.edunex.backend.entity.User;
import com.edunex.backend.enums.EnrollmentStatus;
import com.edunex.backend.enums.NotificationType;
import com.edunex.backend.exception.BadRequestException;
import com.edunex.backend.exception.ConflictException;
import com.edunex.backend.exception.ResourceNotFoundException;
import com.edunex.backend.repository.AdministrationAgentRepository;
import com.edunex.backend.repository.EnrollmentRepository;
import com.edunex.backend.repository.SessionAbsenceRepository;
import com.edunex.backend.repository.TimetableSessionRepository;
import com.edunex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Weekly calendar with absences. A teacher can declare an absence up to N hours before; the administration any time. */
@Service
@RequiredArgsConstructor
public class TeacherAbsenceService {

    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("dd/MM");

    private final TimetableSessionRepository sessionRepository;
    private final SessionAbsenceRepository absenceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AdministrationAgentRepository agentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Value("${app.absence.min-hours-before:10}")
    private int minHours;

    // ---------- week views ----------

    @Transactional(readOnly = true)
    public List<WeekSession> studentWeek(String email, LocalDate any) {
        Student s = user(email, Student.class);
        Set<Long> classIds = enrollmentRepository.findByStudentIdAndStatus(s.getId(), EnrollmentStatus.ACTIVE).stream()
                .map(e -> e.getSchoolClass().getId()).collect(Collectors.toSet());
        return classIds.isEmpty() ? List.of() : week(sessionRepository.findBySchoolClassIdIn(classIds), any, false);
    }

    @Transactional(readOnly = true)
    public List<WeekSession> professorWeek(String email, LocalDate any) {
        Professor p = user(email, Professor.class);
        return week(sessionRepository.findByProfessorId(p.getId()), any, true);
    }

    @Transactional(readOnly = true)
    public List<WeekSession> adminWeek(LocalDate any, Long classId, Long professorId) {
        List<TimetableSession> list = sessionRepository.findAll().stream()
                .filter(s -> classId == null || s.getSchoolClass().getId().equals(classId))
                .filter(s -> professorId == null || s.getProfessor().getId().equals(professorId))
                .toList();
        return week(list, any, false);
    }

    // ---------- declare / mark / cancel ----------

    @Transactional
    public WeekSession declare(String email, Long sessionId, LocalDate date, String reason) {
        Professor p = user(email, Professor.class);
        TimetableSession s = session(sessionId);
        if (!s.getProfessor().getId().equals(p.getId())) {
            throw new AccessDeniedException("This session is not yours");
        }
        checkDate(s, date);
        if (!leadTimeOk(date, s, LocalDateTime.now())) {
            throw new BadRequestException("An absence must be declared at least " + minHours + " hours before the session");
        }
        SessionAbsence a = create(s, date, "PROFESSOR", reason, p);
        String text = "Séance annulée : " + describe(s, date) + " — l'enseignant est absent." + reasonText(reason);
        notificationService.notifyClassStudents(s.getSchoolClass().getId(), NotificationType.TIMETABLE_CHANGE, text);
        notificationService.notifyUsers(agentRepository.findAll(), NotificationType.TIMETABLE_CHANGE,
                "Absence déclarée par " + fullName(p) + " : " + describe(s, date) + "." + reasonText(reason));
        return toWeek(s, date, a, false);
    }

    @Transactional
    public WeekSession markByAdmin(String agentEmail, Long sessionId, LocalDate date, String reason) {
        User agent = userRepository.findByEmail(agentEmail).orElseThrow(() -> new AccessDeniedException("Unknown user"));
        TimetableSession s = session(sessionId);
        checkDate(s, date);
        if (date.isBefore(LocalDate.now().minusDays(14)) || date.isAfter(LocalDate.now().plusDays(120))) {
            throw new BadRequestException("The date is out of the allowed range");
        }
        SessionAbsence a = create(s, date, "ADMIN", reason, agent);
        String text = "Séance annulée : " + describe(s, date) + " — enseignant absent." + reasonText(reason);
        notificationService.notifyClassStudents(s.getSchoolClass().getId(), NotificationType.TIMETABLE_CHANGE, text);
        notificationService.notifyUsers(List.of(s.getProfessor()), NotificationType.TIMETABLE_CHANGE,
                "L'administration a signalé votre absence : " + describe(s, date) + "." + reasonText(reason));
        return toWeek(s, date, a, false);
    }

    @Transactional
    public void cancelOwn(String email, Long absenceId) {
        Professor p = user(email, Professor.class);
        SessionAbsence a = absence(absenceId);
        if (!"PROFESSOR".equals(a.getKind()) || !p.getId().equals(a.getDeclaredById())) {
            throw new AccessDeniedException("Only the administration can cancel this absence");
        }
        TimetableSession s = session(a.getSessionId());
        if (!leadTimeOk(a.getDate(), s, LocalDateTime.now())) {
            throw new BadRequestException("Too late to withdraw this absence, please contact the administration");
        }
        remove(s, a);
    }

    @Transactional
    public void cancelByAdmin(Long absenceId) {
        SessionAbsence a = absence(absenceId);
        remove(session(a.getSessionId()), a);
    }

    // ---------- helpers ----------

    private void remove(TimetableSession s, SessionAbsence a) {
        absenceRepository.delete(a);
        notificationService.notifyClassStudents(s.getSchoolClass().getId(), NotificationType.TIMETABLE_CHANGE,
                "Séance maintenue : " + describe(s, a.getDate()) + ".");
    }

    private SessionAbsence create(TimetableSession s, LocalDate date, String kind, String reason, User by) {
        if (absenceRepository.existsBySessionIdAndDate(s.getId(), date)) {
            throw new ConflictException("This session is already marked absent");
        }
        SessionAbsence a = new SessionAbsence();
        a.setSessionId(s.getId());
        a.setDate(date);
        a.setKind(kind);
        a.setReason(reason == null || reason.isBlank() ? null : reason.trim());
        a.setDeclaredById(by.getId());
        a.setDeclaredBy(fullName(by));
        return absenceRepository.save(a);
    }

    private List<WeekSession> week(List<TimetableSession> sessions, LocalDate any, boolean professorView) {
        LocalDate monday = (any == null ? LocalDate.now() : any).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Map<String, SessionAbsence> absences = new HashMap<>();
        if (!sessions.isEmpty()) {
            absenceRepository.findBySessionIdInAndDateBetween(
                            sessions.stream().map(TimetableSession::getId).toList(), monday, monday.plusDays(6))
                    .forEach(a -> absences.put(a.getSessionId() + "|" + a.getDate(), a));
        }
        LocalDateTime now = LocalDateTime.now();
        return sessions.stream()
                .sorted(Comparator.comparing((TimetableSession s) -> s.getDayOfWeek().getValue())
                        .thenComparing(TimetableSession::getStartTime))
                .map(s -> {
                    LocalDate date = monday.plusDays(s.getDayOfWeek().getValue() - 1L);
                    SessionAbsence a = absences.get(s.getId() + "|" + date);
                    return toWeek(s, date, a, professorView && a == null && leadTimeOk(date, s, now));
                })
                .toList();
    }

    private WeekSession toWeek(TimetableSession s, LocalDate date, SessionAbsence a, boolean canDeclare) {
        Info info = a == null ? null : new Info(a.getId(), a.getKind(), a.getReason(), a.getDeclaredBy(), a.getCreatedAt());
        return new WeekSession(s.getId(), date, s.getDayOfWeek().name(), s.getStartTime().toString(),
                s.getEndTime().toString(), s.getRoom(), s.getSchoolClass().getId(), s.getSchoolClass().getName(),
                s.getSubject().getId(), s.getSubject().getName(), s.getSubject().getCode(),
                s.getProfessor().getId(), fullName(s.getProfessor()), info, canDeclare);
    }

    private boolean leadTimeOk(LocalDate date, TimetableSession s, LocalDateTime now) {
        return !now.isAfter(LocalDateTime.of(date, s.getStartTime()).minusHours(minHours));
    }

    private static void checkDate(TimetableSession s, LocalDate date) {
        if (date == null || date.getDayOfWeek() != s.getDayOfWeek()) {
            throw new BadRequestException("The date does not match the day of this session");
        }
    }

    private static String describe(TimetableSession s, LocalDate date) {
        return s.getSubject().getName() + ", " + s.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH) + " "
                + date.format(SHORT) + " de " + s.getStartTime() + " à " + s.getEndTime();
    }

    private static String reasonText(String reason) {
        return reason == null || reason.isBlank() ? "" : " Motif : " + reason.trim();
    }

    private static String fullName(User u) {
        return u.getFirstName() + " " + u.getLastName();
    }

    private TimetableSession session(Long id) {
        return sessionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Session not found"));
    }

    private SessionAbsence absence(Long id) {
        return absenceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Absence not found"));
    }

    private <T extends User> T user(String email, Class<T> type) {
        return userRepository.findByEmail(email).filter(type::isInstance).map(type::cast)
                .orElseThrow(() -> new AccessDeniedException(type.getSimpleName() + " account required"));
    }
}