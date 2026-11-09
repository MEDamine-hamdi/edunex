package com.edunex.backend.controller;

import com.edunex.backend.dto.SessionRequest;
import com.edunex.backend.dto.TimetableRequest;
import com.edunex.backend.repository.SchoolClassRepository;
import com.edunex.backend.repository.TimetableRepository;
import com.edunex.backend.service.TimetableService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

/** A person who was given the right to manage timetables: create a timetable and add sessions (conflicts are refused). */
@RestController
@RequestMapping("/api/delegated/timetable")
@PreAuthorize("hasAuthority('TIMETABLE_MANAGE')")
@RequiredArgsConstructor
public class DelegatedTimetableController {

    public record Prof(Long id, String name) {
    }

    public record SubjectItem(Long id, String name, String code, List<Prof> professors) {
    }

    public record ClassItem(Long id, String name, List<SubjectItem> subjects) {
    }

    public record TimetableItem(Long id, String label) {
    }

    public record Lookups(List<TimetableItem> timetables, List<ClassItem> classes) {
    }

    public record NewTimetable(String academicYear, String semester) {
    }

    public record NewSession(Long timetableId, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, String room,
                             Long classId, Long subjectId, Long professorId) {
    }

    private final TimetableRepository timetableRepository;
    private final SchoolClassRepository classRepository;
    private final TimetableService timetableService;

    @GetMapping("/lookups")
    @Transactional(readOnly = true)
    public Lookups lookups() {
        return new Lookups(
                timetableRepository.findAll().stream()
                        .map(t -> new TimetableItem(t.getId(), t.getAcademicYear() + " - " + t.getSemester())).toList(),
                classRepository.findAll().stream().sorted(Comparator.comparing(c -> c.getName()))
                        .map(c -> new ClassItem(c.getId(), c.getName(), c.getSubjects().stream()
                                .sorted(Comparator.comparing(s -> s.getName()))
                                .map(s -> new SubjectItem(s.getId(), s.getName(), s.getCode(), s.getProfessors().stream()
                                        .map(p -> new Prof(p.getId(), p.getFirstName() + " " + p.getLastName())).toList()))
                                .toList()))
                        .toList());
    }

    @PostMapping("/timetables")
    public ResponseEntity<Object> createTimetable(@RequestBody NewTimetable request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(timetableService.create(new TimetableRequest(request.academicYear(), request.semester())));
    }

    @PostMapping("/sessions")
    public ResponseEntity<Object> addSession(@RequestBody NewSession r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(timetableService.addSession(r.timetableId(),
                new SessionRequest(r.dayOfWeek(), r.startTime(), r.endTime(), r.room(), r.classId(), r.subjectId(), r.professorId())));
    }
}