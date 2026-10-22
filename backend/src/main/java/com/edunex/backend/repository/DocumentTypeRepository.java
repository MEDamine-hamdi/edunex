package com.edunex.backend.repository;

import com.edunex.backend.entity.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentTypeRepository extends JpaRepository<DocumentType, Long> {

    List<DocumentType> findAllByOrderByNameAsc();

    Optional<DocumentType> findByNameIgnoreCase(String name);
}