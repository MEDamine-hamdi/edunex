package com.edunex.backend.repository;

import com.edunex.backend.entity.TimetableSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.Collection;
import java.util.List;

@Repository
public interface TimetableSessionRepository extends JpaRepository<TimetableSession, Long> {

    List<TimetableSession> findByTimetableId(Long timetableId);

    List<TimetableSession> findByTimetableIdAndDayOfWeek(Long timetableId, DayOfWeek dayOfWeek);

    List<TimetableSession> findByProfessorId(Long professorId);

    List<TimetableSession> findBySchoolClassIdIn(Collection<Long> classIds);
}
