package com.edunex.backend.repository;

import com.edunex.backend.entity.Enrollment;
import com.edunex.backend.enums.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByStudentId(Long studentId);

    List<Enrollment> findByStudentIdAndStatus(Long studentId, EnrollmentStatus status);

    List<Enrollment> findBySchoolClassId(Long classId);

    boolean existsByStudentIdAndSchoolClassIdAndAcademicYearAndStatus(
            Long studentId, Long classId, String academicYear, EnrollmentStatus status);
}
