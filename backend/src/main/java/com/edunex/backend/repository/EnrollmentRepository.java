package com.edunex.backend.repository;

import com.edunex.backend.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStudentId(Long studentId);
    List<Enrollment> findByStudentIdAndStatus(Long studentId, com.edunex.backend.enums.EnrollmentStatus status);
}
