package com.edunex.backend.repository;

import com.edunex.backend.entity.CourseCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseCompletionRepository extends JpaRepository<CourseCompletion, Long> {

    Optional<CourseCompletion> findByStudentIdAndCourseId(Long studentId, Long courseId);
}