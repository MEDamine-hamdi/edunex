package com.edunex.backend.service;

import com.edunex.backend.entity.Enrollment;
import com.edunex.backend.entity.Subject;
import com.edunex.backend.enums.EnrollmentStatus;
import com.edunex.backend.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Single source of truth for "which subjects can this student access?".
 * Rule: subjects of every class where the student has an ACTIVE enrollment.
 */
@Service
@RequiredArgsConstructor
public class StudentAccessService {

    private final EnrollmentRepository enrollmentRepository;

    @Transactional(readOnly = true)
    public Set<Long> accessibleSubjectIds(Long studentId) {
        Set<Long> ids = new HashSet<>();
        for (Enrollment e : enrollmentRepository.findByStudentIdAndStatus(studentId, EnrollmentStatus.ACTIVE)) {
            for (Subject s : e.getSchoolClass().getSubjects()) {
                ids.add(s.getId());
            }
        }
        return ids;
    }
}
