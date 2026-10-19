package com.edunex.backend.repository;

import com.edunex.backend.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocumentIdIn(Collection<Long> documentIds);

    @Modifying
    @Query("delete from DocumentChunk c where c.documentId = :documentId")
    void deleteByDocument(@Param("documentId") Long documentId);

    @Modifying
    @Query("delete from DocumentChunk c where c.documentId not in (select d.id from CourseDocument d)")
    int deleteOrphans();
}