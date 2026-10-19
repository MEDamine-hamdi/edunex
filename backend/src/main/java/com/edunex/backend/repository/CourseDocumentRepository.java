package com.edunex.backend.repository;

import com.edunex.backend.entity.CourseDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseDocumentRepository extends JpaRepository<CourseDocument, Long> {

    List<CourseDocument> findByCourseId(Long courseId);

    @Query("select d from CourseDocument d join fetch d.course c join fetch c.subject "
            + "where d.id not in (select e.documentId from DocumentIndexEntry e)")
    List<CourseDocument> findNotIndexed();
}