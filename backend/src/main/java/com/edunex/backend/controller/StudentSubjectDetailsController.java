package com.edunex.backend.controller;

import com.edunex.backend.entity.Subject;
import com.edunex.backend.repository.SubjectRepository;
import com.edunex.backend.repository.UserRepository;
import com.edunex.backend.service.StudentAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

/** The subjects of the student's classes with their professors (name and e-mail). */
@RestController
@RequiredArgsConstructor
public class StudentSubjectDetailsController {

    public record Prof(String name, String email) {
    }

    public record SubjectCard(Long id, String name, String code, Double coefficient, Integer credits, List<Prof> professors) {
    }

    private final UserRepository userRepository;
    private final StudentAccessService access;
    private final SubjectRepository subjectRepository;

    @GetMapping("/api/student/subjects/details")
    @PreAuthorize("hasRole('STUDENT')")
    @Transactional(readOnly = true)
    public List<SubjectCard> details(Authentication auth) {
        Long studentId = userRepository.findByEmail(auth.getName()).orElseThrow().getId();
        return subjectRepository.findAllById(access.accessibleSubjectIds(studentId)).stream()
                .sorted(Comparator.comparing(Subject::getName))
                .map(s -> new SubjectCard(s.getId(), s.getName(), s.getCode(), s.getCoefficient(), s.getCredits(),
                        s.getProfessors().stream()
                                .sorted(Comparator.comparing(p -> p.getLastName()))
                                .map(p -> new Prof(p.getFirstName() + " " + p.getLastName(), p.getEmail()))
                                .toList()))
                .toList();
    }
}