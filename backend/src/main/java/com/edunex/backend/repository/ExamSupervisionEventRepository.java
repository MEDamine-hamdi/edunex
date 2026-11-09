package com.edunex.backend.repository;

import com.edunex.backend.entity.ExamSupervisionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamSupervisionEventRepository extends JpaRepository<ExamSupervisionEvent, Long> {

    List<ExamSupervisionEvent> findTop300ByOrderByAtDesc();

    List<ExamSupervisionEvent> findTop300ByProfessorIdOrderByAtDesc(Long professorId);
}