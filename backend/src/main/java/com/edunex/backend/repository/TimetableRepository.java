package com.edunex.backend.repository;

import com.edunex.backend.entity.Timetable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TimetableRepository extends JpaRepository<Timetable, Long> {
    Optional<Timetable> findByAcademicYearAndSemester(String academicYear, String semester);
}
