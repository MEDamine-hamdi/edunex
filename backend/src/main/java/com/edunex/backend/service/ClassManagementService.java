package com.edunex.backend.service;

import com.edunex.backend.dto.ClassMgmtDto.AddResult;
import com.edunex.backend.dto.ClassMgmtDto.AddStudents;
import com.edunex.backend.dto.ClassMgmtDto.Candidate;
import com.edunex.backend.dto.ClassMgmtDto.GroupRow;
import com.edunex.backend.dto.ClassMgmtDto.Overview;
import com.edunex.backend.dto.ClassMgmtDto.ProfRef;
import com.edunex.backend.dto.ClassMgmtDto.ProfessorRow;
import com.edunex.backend.dto.ClassMgmtDto.StudentRow;
import com.edunex.backend.dto.ClassMgmtDto.SubjectRow;
import com.edunex.backend.entity.ClassTeaching;
import com.edunex.backend.entity.Enrollment;
import com.edunex.backend.entity.Professor;
import com.edunex.backend.entity.SchoolClass;
import com.edunex.backend.entity.Section;
import com.edunex.backend.entity.Student;
import com.edunex.backend.entity.Subject;
import com.edunex.backend.entity.User;
import com.edunex.backend.enums.EnrollmentStatus;
import com.edunex.backend.enums.UserStatus;
import com.edunex.backend.exception.BadRequestException;
import com.edunex.backend.exception.ResourceNotFoundException;
import com.edunex.backend.repository.ClassTeachingRepository;
import com.edunex.backend.repository.EnrollmentRepository;
import com.edunex.backend.repository.ProfessorRepository;
import com.edunex.backend.repository.SchoolClassRepository;
import com.edunex.backend.repository.SectionRepository;
import com.edunex.backend.repository.StudentRepository;
import com.edunex.backend.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Everything an agent needs to run one class: students, groups, and who teaches each subject. */
@Service
@RequiredArgsConstructor
public class ClassManagementService {

    private final SchoolClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final ProfessorRepository professorRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SectionRepository sectionRepository;
    private final ClassTeachingRepository teachingRepository;

    @Transactional(readOnly = true)
    public Overview overview(Long classId) {
        SchoolClass c = cls(classId);
        List<Enrollment> enrollments = enrollmentRepository.findBySchoolClassIdInAndStatus(List.of(classId), EnrollmentStatus.ACTIVE);

        Map<Long, Long> perGroup = enrollments.stream().filter(e -> e.getSection() != null)
                .collect(Collectors.groupingBy(e -> e.getSection().getId(), Collectors.counting()));
        List<GroupRow> groups = sectionRepository.findBySchoolClassId(classId).stream()
                .sorted(Comparator.comparing(Section::getName))
                .map(s -> new GroupRow(s.getId(), s.getName(), perGroup.getOrDefault(s.getId(), 0L).intValue()))
                .toList();

        Map<Long, List<ClassTeaching>> explicit = teachingRepository.findByClassId(classId).stream()
                .collect(Collectors.groupingBy(ClassTeaching::getSubjectId));
        Map<Long, Professor> explicitProfs = new HashMap<>();
        professorRepository.findAllById(explicit.values().stream().flatMap(List::stream)
                .map(ClassTeaching::getProfessorId).collect(Collectors.toSet()))
                .forEach(p -> explicitProfs.put(p.getId(), p));

        List<SubjectRow> subjects = new ArrayList<>();
        Map<Long, Professor> teachers = new HashMap<>();
        Map<Long, Set<String>> taught = new HashMap<>();
        for (Subject s : c.getSubjects().stream().sorted(Comparator.comparing(Subject::getName)).toList()) {
            boolean custom = explicit.containsKey(s.getId());
            List<Professor> raw = custom
                    ? explicit.get(s.getId()).stream().map(t -> explicitProfs.get(t.getProfessorId())).filter(Objects::nonNull).toList()
                    : new ArrayList<>(s.getProfessors());
            List<Professor> sorted = raw.stream()
                    .sorted(Comparator.comparing(Professor::getLastName).thenComparing(Professor::getFirstName)).toList();
            subjects.add(new SubjectRow(s.getId(), s.getName(), s.getCode(),
                    sorted.stream().map(p -> new ProfRef(p.getId(), name(p))).toList(), custom));
            for (Professor p : sorted) {
                teachers.put(p.getId(), p);
                taught.computeIfAbsent(p.getId(), k -> new TreeSet<>()).add(s.getName());
            }
        }
        List<ProfessorRow> professors = teachers.values().stream()
                .sorted(Comparator.comparing(Professor::getLastName).thenComparing(Professor::getFirstName))
                .map(p -> new ProfessorRow(p.getId(), name(p), p.getEmail(), List.copyOf(taught.get(p.getId()))))
                .toList();

        List<StudentRow> students = enrollments.stream()
                .sorted(Comparator.comparing((Enrollment e) -> e.getStudent().getLastName())
                        .thenComparing(e -> e.getStudent().getFirstName()))
                .map(e -> new StudentRow(e.getId(), e.getStudent().getId(), name(e.getStudent()),
                        e.getStudent().getStudentNumber(), e.getStudent().getEmail(),
                        e.getSection() != null ? e.getSection().getId() : null,
                        e.getSection() != null ? e.getSection().getName() : null))
                .toList();
        return new Overview(c.getId(), c.getName(), c.getLevel(), c.getAcademicYear(), groups, subjects, professors, students);
    }

    @Transactional(readOnly = true)
    public List<ProfRef> professorOptions() {
        return professorRepository.findAll().stream()
                .filter(p -> p.getStatus() == UserStatus.ACTIVE)
                .sorted(Comparator.comparing(Professor::getLastName).thenComparing(Professor::getFirstName))
                .map(p -> new ProfRef(p.getId(), name(p)))
                .toList();
    }

    /** Sets the professors of one subject for this class. An empty list goes back to the subject's general professors. */
    @Transactional
    public Overview setProfessors(Long classId, Long subjectId, List<Long> professorIds) {
        SchoolClass c = cls(classId);
        Subject s = c.getSubjects().stream().filter(x -> x.getId().equals(subjectId)).findFirst()
                .orElseThrow(() -> new BadRequestException("This subject is not taught in this class"));
        Set<Long> ids = new LinkedHashSet<>(professorIds == null ? List.of() : professorIds);
        List<Professor> profs = professorRepository.findAllById(ids);
        if (profs.size() != ids.size()) {
            throw new ResourceNotFoundException("One or more professors not found");
        }
        teachingRepository.deleteByClassAndSubject(classId, subjectId);
        for (Professor p : profs) {
            ClassTeaching t = new ClassTeaching();
            t.setClassId(classId);
            t.setSubjectId(subjectId);
            t.setProfessorId(p.getId());
            teachingRepository.save(t);
            if (s.getProfessors().stream().noneMatch(x -> x.getId().equals(p.getId()))) {
                s.getProfessors().add(p);
            }
        }
        subjectRepository.save(s);
        return overview(classId);
    }

    @Transactional(readOnly = true)
    public List<Candidate> candidates(Long classId, String q) {
        cls(classId);
        Set<Long> enrolled = enrollmentRepository.findBySchoolClassIdInAndStatus(List.of(classId), EnrollmentStatus.ACTIVE)
                .stream().map(e -> e.getStudent().getId()).collect(Collectors.toSet());
        String needle = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        return studentRepository.findAll().stream()
                .filter(s -> s.getStatus() == UserStatus.ACTIVE && !enrolled.contains(s.getId()))
                .filter(s -> needle.isEmpty() || (name(s) + " " + s.getEmail() + " " + s.getStudentNumber())
                        .toLowerCase(Locale.ROOT).contains(needle))
                .sorted(Comparator.comparing(Student::getLastName).thenComparing(Student::getFirstName))
                .limit(100)
                .map(s -> new Candidate(s.getId(), name(s), s.getStudentNumber(), s.getEmail()))
                .toList();
    }

    @Transactional
    public AddResult addStudents(Long classId, AddStudents req) {
        SchoolClass c = cls(classId);
        Section group = req.groupId() == null ? null : group(classId, req.groupId());
        int added = 0;
        int skipped = 0;
        List<String> messages = new ArrayList<>();
        for (Long id : new LinkedHashSet<>(req.studentIds())) {
            Student s = studentRepository.findById(id).orElse(null);
            if (s == null || s.getStatus() != UserStatus.ACTIVE) {
                skipped++;
                messages.add("Étudiant " + id + " introuvable ou inactif");
                continue;
            }
            if (enrollmentRepository.existsByStudentIdAndSchoolClassIdAndAcademicYearAndStatus(
                    id, classId, c.getAcademicYear(), EnrollmentStatus.ACTIVE)) {
                skipped++;
                continue;
            }
            Enrollment e = new Enrollment();
            e.setStudent(s);
            e.setSchoolClass(c);
            e.setSection(group);
            e.setEnrollmentDate(LocalDate.now());
            e.setAcademicYear(c.getAcademicYear());
            e.setStatus(EnrollmentStatus.ACTIVE);
            enrollmentRepository.save(e);
            added++;
        }
        return new AddResult(added, skipped, messages);
    }

    @Transactional
    public void removeStudent(Long classId, Long studentId) {
        List<Enrollment> list = activeEnrollments(classId, studentId);
        list.forEach(e -> e.setStatus(EnrollmentStatus.CANCELLED));
    }

    @Transactional
    public void changeGroup(Long classId, Long studentId, Long groupId) {
        Section g = groupId == null ? null : group(classId, groupId);
        activeEnrollments(classId, studentId).forEach(e -> e.setSection(g));
    }

    @Transactional
    public void renameGroup(Long classId, Long groupId, String name) {
        group(classId, groupId).setName(name.trim());
    }

    @Transactional
    public void deleteGroup(Long classId, Long groupId) {
        Section g = group(classId, groupId);
        enrollmentRepository.findBySchoolClassId(classId).stream()
                .filter(e -> e.getSection() != null && e.getSection().getId().equals(groupId))
                .forEach(e -> e.setSection(null));
        sectionRepository.delete(g);
    }

    // ---------- helpers ----------

    private List<Enrollment> activeEnrollments(Long classId, Long studentId) {
        List<Enrollment> list = enrollmentRepository.findByStudentIdAndStatus(studentId, EnrollmentStatus.ACTIVE).stream()
                .filter(e -> e.getSchoolClass().getId().equals(classId)).toList();
        if (list.isEmpty()) {
            throw new ResourceNotFoundException("The student is not in this class");
        }
        return list;
    }

    private SchoolClass cls(Long id) {
        return classRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Class not found"));
    }

    private Section group(Long classId, Long groupId) {
        return sectionRepository.findById(groupId)
                .filter(s -> s.getSchoolClass() != null && s.getSchoolClass().getId().equals(classId))
                .orElseThrow(() -> new ResourceNotFoundException("Group not found in this class"));
    }

    private static String name(User u) {
        return u.getFirstName() + " " + u.getLastName();
    }
}