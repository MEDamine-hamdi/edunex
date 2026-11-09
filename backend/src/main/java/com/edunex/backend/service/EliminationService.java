package com.edunex.backend.service;

import com.edunex.backend.entity.AbsencePolicy;
import com.edunex.backend.entity.Attendance;
import com.edunex.backend.entity.Elimination;
import com.edunex.backend.entity.Student;
import com.edunex.backend.entity.Subject;
import com.edunex.backend.entity.User;
import com.edunex.backend.enums.AttendanceStatus;
import com.edunex.backend.enums.NotificationType;
import com.edunex.backend.exception.BadRequestException;
import com.edunex.backend.exception.ResourceNotFoundException;
import com.edunex.backend.repository.AbsencePolicyRepository;
import com.edunex.backend.repository.AdministrationAgentRepository;
import com.edunex.backend.repository.AttendanceRepository;
import com.edunex.backend.repository.EliminationRepository;
import com.edunex.backend.repository.StudentRepository;
import com.edunex.backend.repository.SubjectRepository;
import com.edunex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Eliminates a student from a subject when the absences go above the limit set by the administration. */
@Slf4j
@Service
@RequiredArgsConstructor
public class EliminationService {

    public record PolicyValue(Integer maxAbsences, Integer latesPerAbsence) {
    }

    public record SubjectPolicy(Long subjectId, String name, String code, PolicyValue override) {
    }

    public record PolicyView(PolicyValue defaultPolicy, List<SubjectPolicy> subjects) {
    }

    public record Row(Long id, String studentName, String studentNumber, String subjectName, String subjectCode,
                      int absences, int max, LocalDateTime eliminatedAt, boolean waived, String waivedBy, String waiveReason) {
    }

    public record StudentRow(Long subjectId, String name, String code, int absences, int absent, int late,
                             Integer max, Integer remaining, boolean eliminated, boolean waived) {
    }

    private final AbsencePolicyRepository policies;
    private final EliminationRepository eliminations;
    private final AttendanceRepository attendanceRepository;
    private final SubjectRepository subjectRepository;
    private final StudentRepository studentRepository;
    private final AdministrationAgentRepository agentRepository;
    private final UserRepository userRepository;
    private final StudentAccessService studentAccessService;
    private final NotificationService notificationService;

    static int effective(int absent, int late, int latesPerAbsence) {
        return absent + (latesPerAbsence > 0 ? late / latesPerAbsence : 0);
    }

    private Optional<AbsencePolicy> policyFor(Long subjectId) {
        return policies.findBySubjectId(subjectId).or(() -> policies.findBySubjectIdIsNull()).filter(p -> p.getMaxAbsences() > 0);
    }

    // ---------- the check ----------

    @Scheduled(initialDelayString = "PT2M", fixedDelayString = "PT2M")
    public void scheduled() {
        try {
            recomputeAll();
        } catch (Exception e) {
            log.warn("Elimination check failed: {}", e.toString());
        }
    }

    @Transactional
    public void recomputeAll() {
        for (Subject s : subjectRepository.findAll()) {
            recompute(s);
        }
    }

    private void recompute(Subject subject) {
        Optional<AbsencePolicy> policy = policyFor(subject.getId());
        List<Elimination> existing = eliminations.findBySubjectId(subject.getId());
        if (policy.isEmpty()) {
            existing.stream().filter(e -> e.getWaivedAt() == null).forEach(eliminations::delete);
            return;
        }
        int max = policy.get().getMaxAbsences();
        int per = policy.get().getLatesPerAbsence();
        Map<Long, int[]> counts = new HashMap<>();
        Map<Long, Student> students = new HashMap<>();
        for (Attendance a : attendanceRepository.findBySubjectId(subject.getId())) {
            int[] c = counts.computeIfAbsent(a.getStudent().getId(), k -> new int[2]);
            if (a.getStatus() == AttendanceStatus.ABSENT) {
                c[0]++;
            } else if (a.getStatus() == AttendanceStatus.LATE) {
                c[1]++;
            }
            students.putIfAbsent(a.getStudent().getId(), a.getStudent());
        }
        Map<Long, Elimination> byStudent = existing.stream().collect(Collectors.toMap(Elimination::getStudentId, e -> e));
        for (Map.Entry<Long, int[]> entry : counts.entrySet()) {
            int eff = effective(entry.getValue()[0], entry.getValue()[1], per);
            Elimination e = byStudent.remove(entry.getKey());
            if (eff > max) {
                if (e == null) {
                    Elimination n = new Elimination();
                    n.setStudentId(entry.getKey());
                    n.setSubjectId(subject.getId());
                    n.setAbsences(eff);
                    n.setMaxAbsences(max);
                    eliminations.save(n);
                    announce(students.get(entry.getKey()), subject, eff, max);
                } else {
                    e.setAbsences(eff);
                    e.setMaxAbsences(max);
                }
            } else if (e != null && e.getWaivedAt() == null) {
                eliminations.delete(e);
            }
        }
        byStudent.values().stream().filter(x -> x.getWaivedAt() == null).forEach(eliminations::delete);
    }

    private void announce(Student student, Subject subject, int absences, int max) {
        List<User> who = new ArrayList<>();
        who.add(student);
        notificationService.notifyUsers(who, NotificationType.ATTENDANCE_UPDATE,
                "Vous avez dépassé le nombre d'absences autorisées en " + subject.getName() + " (" + absences
                        + " absences, maximum " + max + ") : vous êtes éliminé(e) de cette matière.");
        List<User> staff = new ArrayList<>(subject.getProfessors());
        staff.addAll(agentRepository.findAll());
        notificationService.notifyUsers(staff, NotificationType.ATTENDANCE_UPDATE,
                student.getFirstName() + " " + student.getLastName() + " est éliminé(e) de " + subject.getName()
                        + " (" + absences + " absences, maximum " + max + ").");
    }

    // ---------- administration ----------

    @Transactional(readOnly = true)
    public PolicyView policy() {
        PolicyValue def = policies.findBySubjectIdIsNull().map(p -> new PolicyValue(p.getMaxAbsences(), p.getLatesPerAbsence())).orElse(null);
        List<SubjectPolicy> list = subjectRepository.findAll().stream().sorted(Comparator.comparing(Subject::getName))
                .map(s -> new SubjectPolicy(s.getId(), s.getName(), s.getCode(),
                        policies.findBySubjectId(s.getId()).map(p -> new PolicyValue(p.getMaxAbsences(), p.getLatesPerAbsence())).orElse(null)))
                .toList();
        return new PolicyView(def, list);
    }

    @Transactional
    public PolicyView setPolicy(Long subjectId, Integer max, Integer lates) {
        int m = max == null ? 0 : max;
        int l = lates == null ? 0 : lates;
        if (m < 0 || m > 99 || l < 0 || l > 20) {
            throw new BadRequestException("Invalid values");
        }
        if (subjectId != null) {
            subjectRepository.findById(subjectId).orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        }
        Optional<AbsencePolicy> existing = subjectId == null ? policies.findBySubjectIdIsNull() : policies.findBySubjectId(subjectId);
        if (m == 0 && subjectId == null) {
            existing.ifPresent(policies::delete);
        } else if (m > 0) {
            AbsencePolicy p = existing.orElseGet(AbsencePolicy::new);
            p.setSubjectId(subjectId);
            p.setMaxAbsences(m);
            p.setLatesPerAbsence(l);
            policies.save(p);
        }
        recomputeAll();
        return policy();
    }

    @Transactional
    public PolicyView removeOverride(Long subjectId) {
        policies.findBySubjectId(subjectId).ifPresent(policies::delete);
        recomputeAll();
        return policy();
    }

    @Transactional(readOnly = true)
    public List<Row> list() {
        Map<Long, Subject> subjects = subjectRepository.findAll().stream().collect(Collectors.toMap(Subject::getId, s -> s));
        Map<Long, Student> students = studentRepository.findAll().stream().collect(Collectors.toMap(Student::getId, s -> s));
        return eliminations.findAllByOrderByEliminatedAtDesc().stream()
                .filter(e -> subjects.containsKey(e.getSubjectId()) && students.containsKey(e.getStudentId()))
                .map(e -> {
                    Student st = students.get(e.getStudentId());
                    Subject su = subjects.get(e.getSubjectId());
                    return new Row(e.getId(), st.getFirstName() + " " + st.getLastName(), st.getStudentNumber(), su.getName(),
                            su.getCode(), e.getAbsences(), e.getMaxAbsences(), e.getEliminatedAt(), e.getWaivedAt() != null,
                            e.getWaivedBy(), e.getWaiveReason());
                }).toList();
    }

    @Transactional
    public void waive(Long id, String agentEmail, String reason) {
        Elimination e = eliminations.findById(id).orElseThrow(() -> new ResourceNotFoundException("Elimination not found"));
        User agent = userRepository.findByEmail(agentEmail).orElseThrow();
        e.setWaivedAt(LocalDateTime.now());
        e.setWaivedBy(agent.getFirstName() + " " + agent.getLastName());
        e.setWaiveReason(reason == null || reason.isBlank() ? null : reason.trim());
        studentRepository.findById(e.getStudentId()).ifPresent(st -> {
            List<User> who = new ArrayList<>();
            who.add(st);
            subjectRepository.findById(e.getSubjectId()).ifPresent(su -> notificationService.notifyUsers(who,
                    NotificationType.ATTENDANCE_UPDATE, "Votre élimination en " + su.getName() + " a été levée par l'administration."));
        });
    }

    @Transactional
    public void restore(Long id) {
        Elimination e = eliminations.findById(id).orElseThrow(() -> new ResourceNotFoundException("Elimination not found"));
        e.setWaivedAt(null);
        e.setWaivedBy(null);
        e.setWaiveReason(null);
    }

    // ---------- the student ----------

    @Transactional(readOnly = true)
    public List<StudentRow> forStudent(String email) {
        Long studentId = userRepository.findByEmail(email).orElseThrow().getId();
        List<StudentRow> rows = new ArrayList<>();
        for (Subject s : subjectRepository.findAllById(studentAccessService.accessibleSubjectIds(studentId))) {
            int absent = 0;
            int late = 0;
            for (Attendance a : attendanceRepository.findByStudentIdAndSubjectId(studentId, s.getId())) {
                if (a.getStatus() == AttendanceStatus.ABSENT) {
                    absent++;
                } else if (a.getStatus() == AttendanceStatus.LATE) {
                    late++;
                }
            }
            Optional<AbsencePolicy> p = policyFor(s.getId());
            int eff = effective(absent, late, p.map(AbsencePolicy::getLatesPerAbsence).orElse(0));
            Integer max = p.map(AbsencePolicy::getMaxAbsences).orElse(null);
            boolean waived = eliminations.findByStudentIdAndSubjectId(studentId, s.getId()).map(e -> e.getWaivedAt() != null).orElse(false);
            boolean eliminated = max != null && eff > max && !waived;
            rows.add(new StudentRow(s.getId(), s.getName(), s.getCode(), eff, absent, late, max,
                    max == null ? null : Math.max(0, max - eff), eliminated, waived));
        }
        rows.sort(Comparator.comparing(StudentRow::name));
        return rows;
    }
}