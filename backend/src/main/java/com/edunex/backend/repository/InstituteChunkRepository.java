package com.edunex.backend.repository;

import com.edunex.backend.entity.InstituteChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface InstituteChunkRepository extends JpaRepository<InstituteChunk, Long> {

    List<InstituteChunk> findByDocumentIdIn(Collection<Long> documentIds);

    @Modifying
    @Query("delete from InstituteChunk c where c.documentId = :documentId")
    void deleteByDocument(@Param("documentId") Long documentId);
}