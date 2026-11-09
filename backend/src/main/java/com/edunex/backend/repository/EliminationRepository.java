package com.edunex.backend.repository;

import com.edunex.backend.entity.Elimination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EliminationRepository extends JpaRepository<Elimination, Long> {

    List<Elimination> findBySubjectId(Long subjectId);

    Optional<Elimination> findByStudentIdAndSubjectId(Long studentId, Long subjectId);

    List<Elimination> findAllByOrderByEliminatedAtDesc();
}