package com.edunex.backend.service;

import com.edunex.backend.dto.ProfessorClassResponse;
import com.edunex.backend.entity.Enrollment;
import com.edunex.backend.entity.Professor;
import com.edunex.backend.entity.SchoolClass;
import com.edunex.backend.entity.Subject;
import com.edunex.backend.enums.EnrollmentStatus;
import com.edunex.backend.repository.EnrollmentRepository;
import com.edunex.backend.repository.SchoolClassRepository;
import com.edunex.backend.repository.SubjectRepository;
import com.edunex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfessorClassService {

    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final SchoolClassRepository classRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional(readOnly = true)
    public List<ProfessorClassResponse> myClasses(String email) {
        Professor p = userRepository.findByEmail(email)
                .filter(u -> u instanceof Professor)
                .map(u -> (Professor) u)
                .orElseThrow(() -> new AccessDeniedException("Professor account required"));

        Map<Long, SchoolClass> classes = new LinkedHashMap<>();
        Map<Long, List<Subject>> subjectsByClass = new HashMap<>();
        for (Subject s : subjectRepository.findByProfessorsId(p.getId())) {
            for (SchoolClass c : classRepository.findBySubjectsId(s.getId())) {
                classes.putIfAbsent(c.getId(), c);
                subjectsByClass.computeIfAbsent(c.getId(), k -> new ArrayList<>()).add(s);
            }
        }
        if (classes.isEmpty()) {
            return List.of();
        }

        Map<Long, List<Enrollment>> enrollments = enrollmentRepository
                .findBySchoolClassIdInAndStatus(classes.keySet(), EnrollmentStatus.ACTIVE).stream()
                .collect(Collectors.groupingBy(e -> e.getSchoolClass().getId()));

        return classes.values().stream()
                .sorted(Comparator.comparing(SchoolClass::getName))
                .map(c -> new ProfessorClassResponse(
                        c.getId(), c.getName(), c.getLevel(), c.getAcademicYear(),
                        subjectsByClass.get(c.getId()).stream()
                                .sorted(Comparator.comparing(Subject::getName))
                                .map(s -> new ProfessorClassResponse.SubjectRef(s.getId(), s.getName(), s.getCode()))
                                .toList(),
                        enrollments.getOrDefault(c.getId(), List.of()).stream()
                                .sorted(Comparator.comparing((Enrollment e) -> e.getStudent().getLastName())
                                        .thenComparing(e -> e.getStudent().getFirstName()))
                                .map(e -> new ProfessorClassResponse.StudentRef(
                                        e.getStudent().getId(),
                                        e.getStudent().getFirstName() + " " + e.getStudent().getLastName(),
                                        e.getStudent().getStudentNumber(),
                                        e.getSection() != null ? e.getSection().getName() : null))
                                .toList()))
                .toList();
    }
}