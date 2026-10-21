package com.edunex.backend.service;

import com.edunex.backend.dto.SessionRequest;
import com.edunex.backend.dto.SessionResponse;
import com.edunex.backend.dto.TimetableRequest;
import com.edunex.backend.dto.TimetableResponse;
import com.edunex.backend.entity.Enrollment;
import com.edunex.backend.entity.Professor;
import com.edunex.backend.entity.SchoolClass;
import com.edunex.backend.entity.Student;
import com.edunex.backend.entity.Subject;
import com.edunex.backend.entity.Timetable;
import com.edunex.backend.entity.TimetableSession;
import com.edunex.backend.enums.EnrollmentStatus;
import com.edunex.backend.enums.NotificationType;
import com.edunex.backend.exception.BadRequestException;
import com.edunex.backend.exception.ConflictException;
import com.edunex.backend.exception.ResourceNotFoundException;
import com.edunex.backend.repository.EnrollmentRepository;
import com.edunex.backend.repository.ProfessorRepository;
import com.edunex.backend.repository.SchoolClassRepository;
import com.edunex.backend.repository.SubjectRepository;
import com.edunex.backend.repository.TimetableRepository;
import com.edunex.backend.repository.TimetableSessionRepository;
import com.edunex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimetableService {

    private static final Comparator<TimetableSession> ORDER =
            Comparator.comparing((TimetableSession s) -> s.getDayOfWeek().getValue())
                    .thenComparing(TimetableSession::getStartTime);

    private final TimetableRepository timetableRepository;
    private final TimetableSessionRepository sessionRepository;
    private final SchoolClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final ProfessorRepository professorRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // ---------- administration agent ----------

    @Transactional
    public TimetableResponse create(TimetableRequest req) {
        String year = req.academicYear().trim();
        String semester = req.semester().trim();
        if (timetableRepository.findByAcademicYearAndSemester(year, semester).isPresent()) {
            throw new ConflictException("A timetable already exists for this academic year and semester");
        }
        Timetable t = new Timetable();
        t.setAcademicYear(year);
        t.setSemester(semester);
        return toResponse(timetableRepository.save(t));
    }

    @Transactional(readOnly = true)
    public List<TimetableResponse> findAll() {
        return timetableRepository.findAll().stream()
                .sorted(Comparator.comparing(Timetable::getAcademicYear).thenComparing(Timetable::getSemester))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> sessions(Long timetableId) {
        findTimetable(timetableId);
        return sessionRepository.findByTimetableId(timetableId).stream()
                .sorted(ORDER)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SessionResponse addSession(Long timetableId, SessionRequest req) {
        Timetable t = findTimetable(timetableId);
        TimetableSession s = new TimetableSession();
        s.setTimetable(t);
        apply(s, t, req, null);
        notifySession("Nouvelle séance", s);
        return toResponse(sessionRepository.save(s));
    }

    @Transactional
    public SessionResponse updateSession(Long sessionId, SessionRequest req) {
        TimetableSession s = findSession(sessionId);
        apply(s, s.getTimetable(), req, sessionId);
        notifySession("Séance modifiée", s);
        return toResponse(sessionRepository.save(s));
    }

    @Transactional
    public void deleteSession(Long sessionId) {
        TimetableSession s = findSession(sessionId);
        notifySession("Séance annulée", s);
        s.getTimetable().getSessions().remove(s);
        sessionRepository.delete(s);
    }

    // ---------- professor and student views ----------

    @Transactional(readOnly = true)
    public List<SessionResponse> professorSessions(String email) {
        Professor p = userRepository.findByEmail(email)
                .filter(u -> u instanceof Professor)
                .map(u -> (Professor) u)
                .orElseThrow(() -> new AccessDeniedException("Professor account required"));
        return sessionRepository.findByProfessorId(p.getId()).stream()
                .sorted(ORDER)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> studentSessions(String email) {
        Student st = userRepository.findByEmail(email)
                .filter(u -> u instanceof Student)
                .map(u -> (Student) u)
                .orElseThrow(() -> new AccessDeniedException("Student account required"));
        Set<Long> classIds = enrollmentRepository.findByStudentIdAndStatus(st.getId(), EnrollmentStatus.ACTIVE)
                .stream()
                .map(Enrollment::getSchoolClass)
                .map(SchoolClass::getId)
                .collect(Collectors.toSet());
        if (classIds.isEmpty()) {
            return List.of();
        }
        return sessionRepository.findBySchoolClassIdIn(classIds).stream()
                .sorted(ORDER)
                .map(this::toResponse)
                .toList();
    }

    // ---------- rules ----------

    private void apply(TimetableSession s, Timetable t, SessionRequest req, Long excludeId) {
        if (!req.startTime().isBefore(req.endTime())) {
            throw new BadRequestException("End time must be after start time");
        }
        SchoolClass c = classRepository.findById(req.classId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        Subject subject = subjectRepository.findById(req.subjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        Professor prof = professorRepository.findById(req.professorId())
                .orElseThrow(() -> new ResourceNotFoundException("Professor not found"));

        if (c.getSubjects().stream().noneMatch(x -> x.getId().equals(subject.getId()))) {
            throw new BadRequestException("This subject is not taught in this class");
        }
        if (subject.getProfessors().stream().noneMatch(x -> x.getId().equals(prof.getId()))) {
            throw new BadRequestException("This professor is not assigned to this subject");
        }

        String room = req.room() == null || req.room().isBlank() ? null : req.room().trim();

        for (TimetableSession other : sessionRepository.findByTimetableIdAndDayOfWeek(t.getId(), req.dayOfWeek())) {
            if (excludeId != null && other.getId().equals(excludeId)) {
                continue;
            }
            boolean overlap = req.startTime().isBefore(other.getEndTime())
                    && req.endTime().isAfter(other.getStartTime());
            if (!overlap) {
                continue;
            }
            if (other.getProfessor().getId().equals(prof.getId())) {
                throw new ConflictException("Conflict: the professor already has a session at this time");
            }
            if (room != null && other.getRoom() != null && other.getRoom().equalsIgnoreCase(room)) {
                throw new ConflictException("Conflict: the room is already used at this time");
            }
            if (other.getSchoolClass().getId().equals(c.getId())) {
                throw new ConflictException("Conflict: the class already has a session at this time");
            }
        }

        s.setDayOfWeek(req.dayOfWeek());
        s.setStartTime(req.startTime());
        s.setEndTime(req.endTime());
        s.setRoom(room);
        s.setSchoolClass(c);
        s.setSubject(subject);
        s.setProfessor(prof);
    }

    // ---------- helpers ----------

    private void notifySession(String prefix, TimetableSession s) {
        String day = s.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.FRENCH);
        String msg = prefix + " : " + s.getSubject().getName() + ", " + day + " de " + s.getStartTime()
                + " à " + s.getEndTime() + (s.getRoom() != null ? " (salle " + s.getRoom() + ")" : "");
        notificationService.notifyClassStudents(s.getSchoolClass().getId(), NotificationType.TIMETABLE_CHANGE, msg);
        notificationService.notifyUsers(java.util.List.of(s.getProfessor()), NotificationType.TIMETABLE_CHANGE, msg);
    }

    /** Dry run: the message of the rule a session would break, or null when it can be created. */
    @Transactional(readOnly = true)
    public String validateSession(Long timetableId, SessionRequest req) {
        try {
            apply(new TimetableSession(), findTimetable(timetableId), req, null);
            return null;
        } catch (ConflictException | BadRequestException | ResourceNotFoundException e) {
            return e.getMessage();
        }
    }

    /** Same rules as addSession, without a notification per session (the import sends one summary). */
    @Transactional
    public SessionResponse importSession(Long timetableId, SessionRequest req) {
        Timetable t = findTimetable(timetableId);
        TimetableSession s = new TimetableSession();
        s.setTimetable(t);
        apply(s, t, req, null);
        return toResponse(sessionRepository.save(s));
    }

    private Timetable findTimetable(Long id) {
        return timetableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable not found"));
    }

    private TimetableSession findSession(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
    }

    private TimetableResponse toResponse(Timetable t) {
        return new TimetableResponse(t.getId(), t.getAcademicYear(), t.getSemester(), t.getSessions().size());
    }

    private SessionResponse toResponse(TimetableSession s) {
        Timetable t = s.getTimetable();
        Professor p = s.getProfessor();
        return new SessionResponse(
                s.getId(), t.getId(), t.getAcademicYear(), t.getSemester(),
                s.getDayOfWeek().name(), s.getStartTime().toString(), s.getEndTime().toString(), s.getRoom(),
                s.getSchoolClass().getId(), s.getSchoolClass().getName(),
                s.getSubject().getId(), s.getSubject().getName(), s.getSubject().getCode(),
                p.getId(), p.getFirstName() + " " + p.getLastName());
    }
}
