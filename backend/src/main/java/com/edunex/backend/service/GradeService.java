package com.edunex.backend.service;

import com.edunex.backend.dto.GradeRequest;
import com.edunex.backend.dto.GradeResponse;
import com.edunex.backend.dto.GradeUpdateRequest;
import com.edunex.backend.dto.RosterEntry;
import com.edunex.backend.dto.StudentGradesSummary;
import com.edunex.backend.entity.Enrollment;
import com.edunex.backend.entity.Grade;
import com.edunex.backend.entity.SchoolClass;
import com.edunex.backend.entity.Student;
import com.edunex.backend.entity.Subject;
import com.edunex.backend.enums.EnrollmentStatus;
import com.edunex.backend.exception.BadRequestException;
import com.edunex.backend.exception.ResourceNotFoundException;
import com.edunex.backend.repository.EnrollmentRepository;
import com.edunex.backend.repository.GradeRepository;
import com.edunex.backend.repository.SchoolClassRepository;
import com.edunex.backend.repository.StudentRepository;
import com.edunex.backend.repository.SubjectRepository;
import com.edunex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GradeService {

    private final GradeRepository gradeRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final SchoolClassRepository classRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final StudentAccessService studentAccessService;

    // ---------- administration agent ----------

    @Transactional(readOnly = true)
    public List<RosterEntry> subjectStudents(Long subjectId) {
        subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        List<Long> classIds = classRepository.findBySubjectsId(subjectId).stream()
                .map(SchoolClass::getId)
                .toList();
        if (classIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Enrollment> map = new LinkedHashMap<>();
        for (Enrollment e : enrollmentRepository.findBySchoolClassIdInAndStatus(classIds, EnrollmentStatus.ACTIVE)) {
            map.putIfAbsent(e.getStudent().getId(), e);
        }
        return map.values().stream()
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
    public List<GradeResponse> list(Long subjectId, Long studentId) {
        List<Grade> list;
        if (subjectId != null && studentId != null) {
            list = gradeRepository.findByStudentIdAndSubjectId(studentId, subjectId);
        } else if (subjectId != null) {
            list = gradeRepository.findBySubjectId(subjectId);
        } else if (studentId != null) {
            list = gradeRepository.findByStudentId(studentId);
        } else {
            list = gradeRepository.findAll();
        }
        return sorted(list);
    }

    @Transactional
    public GradeResponse create(GradeRequest req) {
        checkDate(req.date());
        Student student = studentRepository.findById(req.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        Subject subject = subjectRepository.findById(req.subjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        if (!studentAccessService.accessibleSubjectIds(student.getId()).contains(subject.getId())) {
            throw new BadRequestException("Student is not enrolled in this subject");
        }
        Grade g = new Grade();
        g.setStudent(student);
        g.setSubject(subject);
        g.setValue(round2(req.value()));
        g.setEvaluationType(req.evaluationType());
        g.setDate(req.date());
        return toResponse(gradeRepository.save(g));
    }

    @Transactional
    public GradeResponse update(Long id, GradeUpdateRequest req) {
        Grade g = find(id);
        checkDate(req.date());
        g.setValue(round2(req.value()));
        g.setEvaluationType(req.evaluationType());
        g.setDate(req.date());
        return toResponse(gradeRepository.save(g));
    }

    @Transactional
    public void delete(Long id) {
        gradeRepository.delete(find(id));
    }

    // ---------- student (own data only) ----------

    @Transactional(readOnly = true)
    public List<GradeResponse> studentGrades(String email, Long subjectId) {
        Student s = student(email);
        List<Grade> list = subjectId == null
                ? gradeRepository.findByStudentId(s.getId())
                : gradeRepository.findByStudentIdAndSubjectId(s.getId(), subjectId);
        return sorted(list);
    }

    @Transactional(readOnly = true)
    public StudentGradesSummary studentSummary(String email) {
        Student s = student(email);
        List<Grade> grades = gradeRepository.findByStudentId(s.getId());
        Set<Long> ids = new HashSet<>(studentAccessService.accessibleSubjectIds(s.getId()));
        grades.forEach(g -> ids.add(g.getSubject().getId()));
        if (ids.isEmpty()) {
            return new StudentGradesSummary(List.of(), null);
        }
        Map<Long, List<Grade>> bySubject = grades.stream()
                .collect(Collectors.groupingBy(g -> g.getSubject().getId()));

        List<StudentGradesSummary.SubjectAverage> rows = new ArrayList<>();
        double weighted = 0;
        double weights = 0;
        for (Subject sub : subjectRepository.findAllById(ids).stream()
                .sorted(Comparator.comparing(Subject::getName)).toList()) {
            List<Grade> list = bySubject.getOrDefault(sub.getId(), List.of());
            Double average = null;
            if (!list.isEmpty()) {
                double mean = list.stream().mapToDouble(Grade::getValue).average().orElse(0);
                double weight = sub.getCoefficient() == null ? 1.0 : sub.getCoefficient();
                weighted += mean * weight;
                weights += weight;
                average = round2(mean);
            }
            rows.add(new StudentGradesSummary.SubjectAverage(
                    sub.getId(), sub.getName(), sub.getCode(), sub.getCoefficient(), average, list.size()));
        }
        Double overall = weights > 0 ? round2(weighted / weights) : null;
        return new StudentGradesSummary(rows, overall);
    }

    // ---------- helpers ----------

    private Grade find(Long id) {
        return gradeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grade not found"));
    }

    private Student student(String email) {
        return userRepository.findByEmail(email)
                .filter(u -> u instanceof Student)
                .map(u -> (Student) u)
                .orElseThrow(() -> new AccessDeniedException("Student account required"));
    }

    private static void checkDate(LocalDate date) {
        if (date.isAfter(LocalDate.now())) {
            throw new BadRequestException("Date cannot be in the future");
        }
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private List<GradeResponse> sorted(List<Grade> list) {
        return list.stream()
                .sorted(Comparator.comparing(Grade::getDate).reversed().thenComparing(Grade::getId, Comparator.reverseOrder()))
                .map(this::toResponse)
                .toList();
    }

    private GradeResponse toResponse(Grade g) {
        Student s = g.getStudent();
        Subject sub = g.getSubject();
        return new GradeResponse(
                g.getId(), s.getId(), s.getFirstName() + " " + s.getLastName(), s.getStudentNumber(),
                sub.getId(), sub.getName(), sub.getCode(),
                g.getValue(), g.getEvaluationType().name(), g.getDate());
    }
}
