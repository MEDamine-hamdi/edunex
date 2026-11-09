package com.edunex.backend.repository;

import com.edunex.backend.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    List<Exam> findByExamDateBetweenOrderByExamDateAscStartTimeAsc(LocalDate from, LocalDate to);

    List<Exam> findByExamDate(LocalDate date);

    @Query("select distinct e from Exam e join e.classes c where c.id in :classIds and e.examDate between :from and :to order by e.examDate, e.startTime")
    List<Exam> findForClasses(@Param("classIds") Collection<Long> classIds, @Param("from") LocalDate from, @Param("to") LocalDate to);
}