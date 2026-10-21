package com.edunex.backend.repository;

import com.edunex.backend.entity.SessionAbsence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface SessionAbsenceRepository extends JpaRepository<SessionAbsence, Long> {

    List<SessionAbsence> findBySessionIdInAndDateBetween(Collection<Long> sessionIds, LocalDate from, LocalDate to);

    boolean existsBySessionIdAndDate(Long sessionId, LocalDate date);
}