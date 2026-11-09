package com.edunex.backend.service;

import com.edunex.backend.dto.ExamDto.AssignResult;
import com.edunex.backend.dto.ExamDto.AvailableExam;
import com.edunex.backend.dto.ExamDto.ClassItem;
import com.edunex.backend.dto.ExamDto.EventRow;
import com.edunex.backend.dto.ExamDto.ExamItem;
import com.edunex.backend.dto.ExamDto.Lookups;
import com.edunex.backend.dto.ExamDto.MyExam;
import com.edunex.backend.dto.ExamDto.ProfessorRow;
import com.edunex.backend.dto.ExamDto.SaveExam;
import com.edunex.backend.dto.ExamDto.StudentExam;
import com.edunex.backend.dto.ExamDto.SubjectItem;
import com.edunex.backend.dto.ExamDto.SupervisorItem;
import com.edunex.backend.entity.Exam;
import com.edunex.backend.entity.ExamSupervision;
import com.edunex.backend.entity.ExamSupervisionEvent;
import com.edunex.backend.entity.Professor;
import com.edunex.backend.entity.ProfessorKind;
import com.edunex.backend.entity.SchoolClass;
import com.edunex.backend.entity.Subject;
import com.edunex.backend.entity.TimetableSession;
import com.edunex.backend.entity.User;
import com.edunex.backend.enums.EnrollmentStatus;
import com.edunex.backend.enums.NotificationType;
import com.edunex.backend.enums.UserStatus;
import com.edunex.backend.exception.BadRequestException;
import com.edunex.backend.exception.ConflictException;
import com.edunex.backend.exception.ResourceNotFoundException;
import com.edunex.backend.repository.EnrollmentRepository;
import com.edunex.backend.repository.ExamRepository;
import com.edunex.backend.repository.ExamSupervisionEventRepository;
import com.edunex.backend.repository.ExamSupervisionRepository;
import com.edunex.backend.repository.ProfessorKindRepository;
import com.edunex.backend.repository.ProfessorRepository;
import com.edunex.backend.repository.SchoolClassRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Exams and their supervisors. A permanent professor is assigned by the administration; a vacataire chooses.
 * Nobody can supervise two overlapping exams, an exam cannot be over-filled, and every action is kept in the history.
 */
@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository exams;
    private final ExamSupervisionRepository supervisions;
    private final ExamSupervisionEventRepository events;
    private final ProfessorKindRepository kinds;
    private final SubjectRepository subjects;
    private final SchoolClassRepository classes;
    private final ProfessorRepository professors;
    private final UserRepository users;
    private final EnrollmentRepository enrollments;
    private final TimetableSessionRepository sessions;
    private final NotificationService notificationService;

    @Value("${app.exams.signup-hours:24}")
    private int signupHours;

    @Value("${app.exams.withdraw-hours:48}")
    private int withdrawHours;

    // ---------- professors: kind ----------

    public String kindOf(Long professorId) {
        return kinds.findById(professorId).map(ProfessorKind::getKind).orElse("PERMANENT");
    }

    @Transactional
    public void setKind(Long professorId, String kind) {
        String k = kind == null ? "" : kind.trim().toUpperCase();
        if (!k.equals("PERMANENT") && !k.equals("VACATAIRE")) {
            throw new BadRequestException("The kind must be PERMANENT or VACATAIRE");
        }
        professors.findById(professorId).orElseThrow(() -> new ResourceNotFoundException("Professor not found"));
        ProfessorKind row = kinds.findById(professorId).orElseGet(() -> {
            ProfessorKind n = new ProfessorKind();
            n.setProfessorId(professorId);
            return n;
        });
        row.setKind(k);
        kinds.save(row);
    }

    @Transactional(readOnly = true)
    public List<ProfessorRow> professorOverview() {
        LocalDate today = LocalDate.now();
        return professors.findAll().stream()
                .sorted(Comparator.comparing(Professor::getLastName).thenComparing(Professor::getFirstName))
                .map(p -> {
                    List<ExamSupervision> mine = supervisions.findByProfessorId(p.getId());
                    long upcoming = mine.stream().filter(s -> !s.getExam().getExamDate().isBefore(today)).count();
                    return new ProfessorRow(p.getId(), name(p), p.getEmail(), kindOf(p.getId()), upcoming, mine.size() - upcoming);
                }).toList();
    }

    // ---------- administration: exams ----------

    @Transactional(readOnly = true)
    public Lookups lookups() {
        List<ClassItem> cls = classes.findAll().stream().sorted(Comparator.comparing(SchoolClass::getName))
                .map(c -> new ClassItem(c.getId(), c.getName())).toList();
        List<SubjectItem> subs = subjects.findAll().stream().sorted(Comparator.comparing(Subject::getName))
                .map(s -> new SubjectItem(s.getId(), s.getName(), s.getCode(),
                        classes.findBySubjectsId(s.getId()).stream().map(SchoolClass::getId).toList()))
                .toList();
        return new Lookups(subs, cls);
    }

    @Transactional(readOnly = true)
    public List<ExamItem> adminList(LocalDate from, LocalDate to) {
        return items(exams.findByExamDateBetweenOrderByExamDateAscStartTimeAsc(from, to), true);
    }

    @Transactional
    public ExamItem create(SaveExam req) {
        Exam e = new Exam();
        apply(e, req, true);
        Exam saved = exams.save(e);
        String text = saved.getSubject().getName() + " (" + saved.getExamType() + ") " + describe(saved);
        saved.getClasses().forEach(c -> notificationService.notifyClassStudents(c.getId(), NotificationType.TIMETABLE_CHANGE, "Examen planifié : " + text));
        List<User> vacataires = new ArrayList<>();
        for (Professor p : professors.findAll()) {
            if (p.getStatus() == UserStatus.ACTIVE && "VACATAIRE".equals(kindOf(p.getId()))) {
                vacataires.add(p);
            }
        }
        if (!vacataires.isEmpty()) {
            notificationService.notifyUsers(vacataires, NotificationType.TIMETABLE_CHANGE, "Nouvel examen à surveiller : " + text);
        }
        return items(List.of(saved), true).get(0);
    }

    @Transactional
    public ExamItem update(Long id, SaveExam req) {
        Exam e = exam(id);
        if (!"PLANNED".equals(e.getStatus())) {
            throw new ConflictException("A cancelled exam cannot be changed");
        }
        apply(e, req, false);
        String text = e.getSubject().getName() + " (" + e.getExamType() + ") " + describe(e);
        e.getClasses().forEach(c -> notificationService.notifyClassStudents(c.getId(), NotificationType.TIMETABLE_CHANGE, "Examen modifié : " + text));
        List<User> staff = new ArrayList<>();
        supervisions.findByExamId(id).forEach(s -> staff.add(s.getProfessor()));
        if (!staff.isEmpty()) {
            notificationService.notifyUsers(staff, NotificationType.TIMETABLE_CHANGE, "Surveillance modifiée : " + text);
        }
        return items(List.of(e), true).get(0);
    }

    @Transactional
    public ExamItem cancel(Long id, String reason) {
        Exam e = exam(id);
        if (!"CANCELLED".equals(e.getStatus())) {
            e.setStatus("CANCELLED");
            e.setCancelReason(reason == null || reason.isBlank() ? null : reason.trim());
            String text = e.getSubject().getName() + " (" + e.getExamType() + ") " + describe(e)
                    + (e.getCancelReason() != null ? ". Motif : " + e.getCancelReason() : "");
            List<User> staff = new ArrayList<>();
            for (ExamSupervision s : supervisions.findByExamId(id)) {
                log(e, s.getProfessor(), "EXAM_CANCELLED", "système", e.getCancelReason());
                staff.add(s.getProfessor());
                supervisions.delete(s);
            }
            if (!staff.isEmpty()) {
                notificationService.notifyUsers(staff, NotificationType.TIMETABLE_CHANGE, "Examen annulé : " + text);
            }
            e.getClasses().forEach(c -> notificationService.notifyClassStudents(c.getId(), NotificationType.TIMETABLE_CHANGE, "Examen annulé : " + text));
        }
        return items(List.of(e), true).get(0);
    }

    @Transactional
    public AssignResult assign(Long examId, Long professorId, String agentEmail) {
        Exam e = openExam(examId);
        Professor p = professors.findById(professorId).orElseThrow(() -> new ResourceNotFoundException("Professor not found"));
        if (p.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("The professor account is not active");
        }
        String warning = join(e, p, "ASSIGNED", actorName(agentEmail), false);
        List<User> who = new ArrayList<>();
        who.add(p);
        notificationService.notifyUsers(who, NotificationType.TIMETABLE_CHANGE,
                "Vous êtes affecté(e) à la surveillance de l'examen " + e.getSubject().getName() + " " + describe(e) + ".");
        return new AssignResult(items(List.of(e), true).get(0), warning);
    }

    @Transactional
    public ExamItem remove(Long examId, Long professorId, String agentEmail, String reason) {
        Exam e = exam(examId);
        ExamSupervision s = supervisions.findByExamIdAndProfessorId(examId, professorId)
                .orElseThrow(() -> new ResourceNotFoundException("This professor does not supervise this exam"));
        supervisions.delete(s);
        log(e, s.getProfessor(), "REMOVED", actorName(agentEmail), reason);
        List<User> who = new ArrayList<>();
        who.add(s.getProfessor());
        notificationService.notifyUsers(who, NotificationType.TIMETABLE_CHANGE,
                "Vous n'êtes plus affecté(e) à la surveillance de l'examen " + e.getSubject().getName() + " " + describe(e) + ".");
        return items(List.of(e), true).get(0);
    }

    @Transactional(readOnly = true)
    public List<EventRow> history(Long professorId) {
        return (professorId == null ? events.findTop300ByOrderByAtDesc() : events.findTop300ByProfessorIdOrderByAtDesc(professorId))
                .stream().map(ev -> new EventRow(ev.getId(), ev.getExamId(), ev.getExamLabel(), ev.getProfessorId(),
                        ev.getProfessorName(), ev.getAction(), ev.getActor(), ev.getReason(), ev.getAt())).toList();
    }

    // ---------- professors ----------

    @Transactional(readOnly = true)
    public String myKind(String email) {
        return kindOf(professor(email).getId());
    }

    @Transactional(readOnly = true)
    public List<AvailableExam> available(String email) {
        Professor p = professor(email);
        if (!"VACATAIRE".equals(kindOf(p.getId()))) {
            return List.of();
        }
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        Set<Long> mine = supervisions.findByProfessorId(p.getId()).stream().map(s -> s.getExam().getId()).collect(Collectors.toSet());
        List<Exam> open = exams.findByExamDateBetweenOrderByExamDateAscStartTimeAsc(today, today.plusDays(365)).stream()
                .filter(e -> "PLANNED".equals(e.getStatus()) && !mine.contains(e.getId()) && start(e).isAfter(now)).toList();
        Map<Long, ExamItem> byId = items(open, false).stream().collect(Collectors.toMap(ExamItem::id, i -> i));
        List<AvailableExam> out = new ArrayList<>();
        for (Exam e : open) {
            String blocked = hardBlocker(e, p);
            if (blocked == null && now.isAfter(start(e).minusHours(signupHours))) {
                blocked = "Inscriptions closes (" + signupHours + " h avant l'examen)";
            }
            if (blocked == null) {
                blocked = lessonConflict(e, p);
            }
            out.add(new AvailableExam(byId.get(e.getId()), blocked == null, blocked));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<MyExam> mine(String email, LocalDate from, LocalDate to) {
        Professor p = professor(email);
        LocalDateTime now = LocalDateTime.now();
        List<ExamSupervision> list = supervisions.findByProfessorId(p.getId()).stream()
                .filter(s -> !s.getExam().getExamDate().isBefore(from) && !s.getExam().getExamDate().isAfter(to))
                .sorted(Comparator.comparing((ExamSupervision s) -> s.getExam().getExamDate()).thenComparing(s -> s.getExam().getStartTime()))
                .toList();
        Map<Long, ExamItem> byId = items(list.stream().map(ExamSupervision::getExam).toList(), false).stream()
                .collect(Collectors.toMap(ExamItem::id, i -> i));
        return list.stream().map(s -> new MyExam(byId.get(s.getExam().getId()), s.getMode(),
                "VOLUNTEER".equals(s.getMode()) && now.isBefore(start(s.getExam()).minusHours(withdrawHours)),
                start(s.getExam()).isBefore(now))).toList();
    }

    @Transactional
    public ExamItem volunteer(String email, Long examId) {
        Professor p = professor(email);
        if (!"VACATAIRE".equals(kindOf(p.getId()))) {
            throw new AccessDeniedException("Only part-time professors (vacataires) choose the exams they supervise");
        }
        Exam e = openExam(examId);
        if (LocalDateTime.now().isAfter(start(e).minusHours(signupHours))) {
            throw new ConflictException("Registration closes " + signupHours + " hours before the exam");
        }
        join(e, p, "VOLUNTEER", name(p), true);
        return items(List.of(e), false).get(0);
    }

    @Transactional
    public void withdraw(String email, Long examId) {
        Professor p = professor(email);
        ExamSupervision s = supervisions.findByExamIdAndProfessorId(examId, p.getId())
                .orElseThrow(() -> new ResourceNotFoundException("You do not supervise this exam"));
        if (!"VOLUNTEER".equals(s.getMode())) {
            throw new AccessDeniedException("This supervision was set by the administration: ask them to remove it");
        }
        Exam e = s.getExam();
        if (LocalDateTime.now().isAfter(start(e).minusHours(withdrawHours))) {
            throw new ConflictException("Too late to withdraw (" + withdrawHours + " hours before the exam): contact the administration");
        }
        supervisions.delete(s);
        log(e, p, "WITHDRAWN", name(p), null);
    }

    @Transactional(readOnly = true)
    public List<EventRow> myHistory(String email) {
        return history(professor(email).getId());
    }

    // ---------- students ----------

    @Transactional(readOnly = true)
    public List<StudentExam> studentExams(String email, LocalDate from, LocalDate to) {
        Long studentId = users.findByEmail(email).orElseThrow(() -> new AccessDeniedException("Unknown user")).getId();
        Set<Long> classIds = enrollments.findByStudentIdAndStatus(studentId, EnrollmentStatus.ACTIVE).stream()
                .map(en -> en.getSchoolClass().getId()).collect(Collectors.toSet());
        if (classIds.isEmpty()) {
            return List.of();
        }
        return exams.findForClasses(classIds, from, to).stream()
                .map(e -> new StudentExam(e.getId(), e.getExamType(), e.getSubject().getName(), e.getSubject().getCode(),
                        e.getExamDate(), e.getStartTime(), e.getEndTime(), e.getRoom(), e.getStatus())).toList();
    }

    // ---------- helpers ----------

    private void apply(Exam e, SaveExam req, boolean creating) {
        Subject subject = subjects.findById(req.subjectId()).orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        Set<Long> wanted = new HashSet<>(req.classIds());
        Set<SchoolClass> set = new HashSet<>(classes.findAllById(wanted));
        if (set.size() != wanted.size()) {
            throw new ResourceNotFoundException("Class not found");
        }
        for (SchoolClass c : set) {
            if (c.getSubjects().stream().noneMatch(s -> s.getId().equals(subject.getId()))) {
                throw new BadRequestException("The class " + c.getName() + " does not follow this subject");
            }
        }
        if (!req.endTime().isAfter(req.startTime())) {
            throw new BadRequestException("The end time must be after the start time");
        }
        if (creating && req.date().isBefore(LocalDate.now())) {
            throw new BadRequestException("The date is in the past");
        }
        String room = req.room() == null || req.room().isBlank() ? null : req.room().trim();
        for (Exam other : exams.findByExamDate(req.date())) {
            if (other.getId().equals(e.getId()) || !"PLANNED".equals(other.getStatus())
                    || !overlaps(other.getStartTime(), other.getEndTime(), req.startTime(), req.endTime())) {
                continue;
            }
            if (room != null && room.equalsIgnoreCase(other.getRoom())) {
                throw new ConflictException("This room is already used by another exam at this time");
            }
            if (other.getClasses().stream().anyMatch(c -> wanted.contains(c.getId()))) {
                throw new ConflictException("A class already has an exam at this time");
            }
        }
        int current = e.getId() == null ? 0 : supervisions.findByExamId(e.getId()).size();
        if (req.supervisorsNeeded() < current) {
            throw new BadRequestException("This exam already has " + current + " supervisors");
        }
        e.setSubject(subject);
        e.setClasses(set);
        e.setExamType(req.examType().trim());
        e.setExamDate(req.date());
        e.setStartTime(req.startTime());
        e.setEndTime(req.endTime());
        e.setRoom(room);
        e.setSupervisorsNeeded(req.supervisorsNeeded());
    }

    /** Adds the supervision after the checks. Returns a warning (the professor also teaches at that time) when not strict. */
    private String join(Exam e, Professor p, String mode, String actor, boolean strict) {
        if (supervisions.findByExamIdAndProfessorId(e.getId(), p.getId()).isPresent()) {
            throw new ConflictException("Already supervising this exam");
        }
        String hard = hardBlocker(e, p);
        if (hard != null) {
            throw new ConflictException(hard);
        }
        String lesson = lessonConflict(e, p);
        if (lesson != null && strict) {
            throw new ConflictException(lesson);
        }
        ExamSupervision s = new ExamSupervision();
        s.setExam(e);
        s.setProfessor(p);
        s.setMode(mode);
        s.setAssignedBy(actor);
        supervisions.save(s);
        log(e, p, "VOLUNTEER".equals(mode) ? "VOLUNTEERED" : "ASSIGNED", actor, null);
        return lesson;
    }

    private String hardBlocker(Exam e, Professor p) {
        if (supervisions.findByExamId(e.getId()).size() >= e.getSupervisorsNeeded()) {
            return "Complet : cet examen a tous ses surveillants";
        }
        for (ExamSupervision s : supervisions.findByProfessorId(p.getId())) {
            Exam o = s.getExam();
            if (!o.getId().equals(e.getId()) && "PLANNED".equals(o.getStatus()) && o.getExamDate().equals(e.getExamDate())
                    && overlaps(o.getStartTime(), o.getEndTime(), e.getStartTime(), e.getEndTime())) {
                return "Vous surveillez déjà un autre examen à ce moment";
            }
        }
        return null;
    }

    private String lessonConflict(Exam e, Professor p) {
        for (TimetableSession t : sessions.findByProfessorId(p.getId())) {
            if (t.getDayOfWeek() == e.getExamDate().getDayOfWeek() && overlaps(t.getStartTime(), t.getEndTime(), e.getStartTime(), e.getEndTime())) {
                return "Vous avez cours (" + t.getSubject().getName() + ") sur ce créneau";
            }
        }
        return null;
    }

    private List<ExamItem> items(List<Exam> list, boolean withStudents) {
        if (list.isEmpty()) {
            return List.of();
        }
        Map<Long, List<ExamSupervision>> sup = supervisions.findByExamIdIn(list.stream().map(Exam::getId).toList()).stream()
                .collect(Collectors.groupingBy(s -> s.getExam().getId()));
        return list.stream().map(e -> {
            List<SupervisorItem> sv = sup.getOrDefault(e.getId(), List.of()).stream()
                    .sorted(Comparator.comparing((ExamSupervision s) -> s.getProfessor().getLastName()))
                    .map(s -> new SupervisorItem(s.getProfessor().getId(), name(s.getProfessor()), s.getProfessor().getEmail(),
                            kindOf(s.getProfessor().getId()), s.getMode(), s.getAssignedBy())).toList();
            Set<Long> classIds = e.getClasses().stream().map(SchoolClass::getId).collect(Collectors.toSet());
            int students = !withStudents || classIds.isEmpty() ? 0 : (int) enrollments
                    .findBySchoolClassIdInAndStatus(classIds, EnrollmentStatus.ACTIVE).stream()
                    .map(en -> en.getStudent().getId()).distinct().count();
            return new ExamItem(e.getId(), e.getExamType(), e.getSubject().getId(), e.getSubject().getName(), e.getSubject().getCode(),
                    e.getClasses().stream().sorted(Comparator.comparing(SchoolClass::getName)).map(c -> new ClassItem(c.getId(), c.getName())).toList(),
                    e.getExamDate(), e.getStartTime(), e.getEndTime(), e.getRoom(), e.getSupervisorsNeeded(), e.getStatus(),
                    e.getCancelReason(), sv, students);
        }).toList();
    }

    private void log(Exam e, Professor p, String action, String actor, String reason) {
        ExamSupervisionEvent ev = new ExamSupervisionEvent();
        ev.setExamId(e.getId());
        ev.setExamLabel(e.getSubject().getName() + " (" + e.getExamType() + ") " + e.getExamDate() + " " + e.getStartTime());
        ev.setProfessorId(p.getId());
        ev.setProfessorName(name(p));
        ev.setAction(action);
        ev.setActor(actor);
        ev.setReason(reason == null || reason.isBlank() ? null : reason.trim());
        events.save(ev);
    }

    private Exam openExam(Long id) {
        Exam e = exam(id);
        if (!"PLANNED".equals(e.getStatus()) || !start(e).isAfter(LocalDateTime.now())) {
            throw new ConflictException("This exam is cancelled or already over");
        }
        return e;
    }

    private Exam exam(Long id) {
        return exams.findById(id).orElseThrow(() -> new ResourceNotFoundException("Exam not found"));
    }

    private Professor professor(String email) {
        return users.findByEmail(email).filter(u -> u instanceof Professor).map(u -> (Professor) u)
                .orElseThrow(() -> new AccessDeniedException("Professor account required"));
    }

    private String actorName(String email) {
        return users.findByEmail(email).map(ExamService::name).orElse(email);
    }

    private static LocalDateTime start(Exam e) {
        return LocalDateTime.of(e.getExamDate(), e.getStartTime());
    }

    private static boolean overlaps(LocalTime s1, LocalTime e1, LocalTime s2, LocalTime e2) {
        return s1.isBefore(e2) && s2.isBefore(e1);
    }

    private static String describe(Exam e) {
        return "le " + e.getExamDate() + " de " + e.getStartTime() + " à " + e.getEndTime()
                + (e.getRoom() != null ? ", salle " + e.getRoom() : "");
    }

    private static String name(User u) {
        return u.getFirstName() + " " + u.getLastName();
    }
}