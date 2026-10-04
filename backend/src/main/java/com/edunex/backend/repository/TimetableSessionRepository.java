package com.edunex.backend.repository;

import com.edunex.backend.entity.TimetableSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TimetableSessionRepository extends JpaRepository<TimetableSession, Long> {
    List<TimetableSession> findByTimetableId(Long timetableId);
}
