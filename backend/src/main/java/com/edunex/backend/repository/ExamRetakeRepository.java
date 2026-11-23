package com.edunex.backend.repository;

import com.edunex.backend.entity.ExamRetake;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExamRetakeRepository extends JpaRepository<ExamRetake, Long> {

    List<ExamRetake> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    Optional<ExamRetake> findByStudentIdAndExamId(Long studentId, Long examId);

    List<ExamRetake> findByCertificateId(Long certificateId);

    List<ExamRetake> findAllByOrderByCreatedAtDesc();

    List<ExamRetake> findByRetakeDate(LocalDate date);
}