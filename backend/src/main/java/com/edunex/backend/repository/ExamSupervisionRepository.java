package com.edunex.backend.repository;

import com.edunex.backend.entity.ExamSupervision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExamSupervisionRepository extends JpaRepository<ExamSupervision, Long> {

    List<ExamSupervision> findByExamId(Long examId);

    List<ExamSupervision> findByExamIdIn(Collection<Long> examIds);

    List<ExamSupervision> findByProfessorId(Long professorId);

    Optional<ExamSupervision> findByExamIdAndProfessorId(Long examId, Long professorId);
}