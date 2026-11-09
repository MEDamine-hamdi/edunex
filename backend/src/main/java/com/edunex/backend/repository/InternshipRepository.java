package com.edunex.backend.repository;

import com.edunex.backend.entity.Internship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InternshipRepository extends JpaRepository<Internship, Long> {

    List<Internship> findAllByOrderByStartDateDesc();

    List<Internship> findByStudentIdOrderByStartDateDesc(Long studentId);

    List<Internship> findBySupervisorIdOrderByStartDateDesc(Long professorId);
}