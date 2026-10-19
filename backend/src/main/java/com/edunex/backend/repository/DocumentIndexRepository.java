package com.edunex.backend.repository;

import com.edunex.backend.entity.DocumentIndexEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentIndexRepository extends JpaRepository<DocumentIndexEntry, Long> {

    @Modifying
    @Query("delete from DocumentIndexEntry e where e.documentId not in (select d.id from CourseDocument d)")
    int deleteOrphans();
}