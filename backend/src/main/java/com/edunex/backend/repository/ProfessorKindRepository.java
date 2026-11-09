package com.edunex.backend.repository;

import com.edunex.backend.entity.ProfessorKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfessorKindRepository extends JpaRepository<ProfessorKind, Long> {
}