package com.edunex.backend.repository;

import com.edunex.backend.entity.InstituteDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstituteDocumentRepository extends JpaRepository<InstituteDocument, Long> {

    List<InstituteDocument> findAllByOrderByUploadedAtDesc();

    List<InstituteDocument> findByStatus(String status);

    long countByStatus(String status);
}