package com.edunex.backend.repository;

import com.edunex.backend.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findBySubjectId(Long subjectId);

    List<Course> findByProfessorId(Long professorId);

    List<Course> findBySubjectIdIn(Collection<Long> subjectIds);
}
