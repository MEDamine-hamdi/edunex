package com.edunex.backend.controller;

import com.edunex.backend.dto.GradeRequest;
import com.edunex.backend.entity.Professor;
import com.edunex.backend.entity.Subject;
import com.edunex.backend.enums.EvaluationType;
import com.edunex.backend.repository.SubjectRepository;
import com.edunex.backend.repository.UserRepository;
import com.edunex.backend.service.GradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** A professor who was given the right to enter grades: only in the subjects he teaches. */
@RestController
@RequestMapping("/api/delegated/grades")
@PreAuthorize("hasAuthority('GRADES_MANAGE')")
@RequiredArgsConstructor
public class DelegatedGradesController {

    public record SubjectItem(Long id, String name, String code) {
    }

    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final GradeService gradeService;

    @GetMapping("/subjects")
    @Transactional(readOnly = true)
    public List<SubjectItem> subjects(Authentication auth) {
        Professor p = professor(auth);
        return subjectRepository.findByProfessorsId(p.getId()).stream()
                .sorted(Comparator.comparing(Subject::getName))
                .map(s -> new SubjectItem(s.getId(), s.getName(), s.getCode())).toList();
    }

    @GetMapping("/subjects/{id}/students")
    public Object students(@PathVariable Long id, Authentication auth) {
        teaches(auth, id);
        return gradeService.subjectStudents(id);
    }

    @GetMapping("/evaluation-types")
    public List<String> types() {
        return Arrays.stream(EvaluationType.values()).map(Enum::name).toList();
    }

    @PostMapping
    public ResponseEntity<Object> create(@Valid @RequestBody GradeRequest request, Authentication auth) {
        teaches(auth, request.subjectId());
        return ResponseEntity.status(HttpStatus.CREATED).body(gradeService.create(request));
    }

    private Professor professor(Authentication auth) {
        return userRepository.findByEmail(auth.getName()).filter(u -> u instanceof Professor).map(u -> (Professor) u)
                .orElseThrow(() -> new AccessDeniedException("Professor account required"));
    }

    private void teaches(Authentication auth, Long subjectId) {
        Professor p = professor(auth);
        if (subjectRepository.findByProfessorsId(p.getId()).stream().noneMatch(s -> s.getId().equals(subjectId))) {
            throw new AccessDeniedException("You do not teach this subject");
        }
    }
}