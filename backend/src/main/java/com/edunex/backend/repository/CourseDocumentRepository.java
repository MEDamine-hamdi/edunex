package com.edunex.backend.repository;

import com.edunex.backend.entity.CourseDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseDocumentRepository extends JpaRepository<CourseDocument, Long> {
    List<CourseDocument> findByCourseId(Long courseId);
}
