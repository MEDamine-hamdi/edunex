package com.edunex.backend.repository;

import com.edunex.backend.entity.Defense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DefenseRepository extends JpaRepository<Defense, Long> {

    Optional<Defense> findByInternshipId(Long internshipId);

    List<Defense> findAllByOrderByScheduledAtDesc();

    List<Defense> findByInternshipStudentIdOrderByScheduledAtDesc(Long studentId);

    @Query("select distinct d from Defense d left join d.jurors j "
            + "where j.professor.id = :pid or d.internship.supervisor.id = :pid order by d.scheduledAt desc")
    List<Defense> findForProfessor(@Param("pid") Long professorId);
}