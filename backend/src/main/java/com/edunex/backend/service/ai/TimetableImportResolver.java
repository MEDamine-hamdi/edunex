package com.edunex.backend.service.ai;

import com.edunex.backend.dto.ImportDto;
import com.edunex.backend.dto.SessionRequest;
import com.edunex.backend.entity.Professor;
import com.edunex.backend.entity.SchoolClass;
import com.edunex.backend.entity.Subject;
import com.edunex.backend.repository.ProfessorRepository;
import com.edunex.backend.repository.SchoolClassRepository;
import com.edunex.backend.repository.SubjectRepository;
import com.edunex.backend.service.TimetableService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Turns what the AI read into rows tied to real classes, subjects and professors, with the problems listed. */
@Service
@RequiredArgsConstructor
public class TimetableImportResolver {

    private final SchoolClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final ProfessorRepository professorRepository;
    private final TimetableService timetableService;

    @Transactional(readOnly = true)
    public List<ImportDto.Row> resolve(List<TimetableImportService.RawSession> raws, Long defaultClassId, Long timetableId) {
        List<SchoolClass> classes = classRepository.findAll();
        List<Subject> subjects = subjectRepository.findAll();
        List<Professor> professors = professorRepository.findAll();
        SchoolClass fixed = defaultClassId == null ? null
                : classes.stream().filter(c -> c.getId().equals(defaultClassId)).findFirst().orElse(null);

        List<ImportDto.Row> rows = new ArrayList<>();
        int id = 1;
        for (TimetableImportService.RawSession r : raws) {
            List<String> issues = new ArrayList<>();
            DayOfWeek day = TimetableMatcher.day(r.day());
            LocalTime start = TimetableMatcher.time(r.start());
            LocalTime end = TimetableMatcher.time(r.end());
            if (day == null) {
                issues.add("Jour non reconnu");
            }
            if (start == null || end == null || !start.isBefore(end)) {
                issues.add("Horaire invalide");
            }

            SchoolClass cls = fixed != null ? fixed
                    : TimetableMatcher.best(classes, c -> List.of(c.getName()), r.className(), 0.5);
            if (cls == null && classes.size() == 1) {
                cls = classes.get(0);
            }
            if (cls == null) {
                issues.add("Classe introuvable" + quote(r.className()));
            }

            Collection<Subject> subjectPool = cls != null && !cls.getSubjects().isEmpty() ? cls.getSubjects() : subjects;
            Subject subject = TimetableMatcher.best(subjectPool, s -> List.of(s.getName(), s.getCode()), r.subject(), 0.5);
            if (subject == null) {
                issues.add("Matière introuvable" + quote(r.subject()));
            } else if (cls != null && cls.getSubjects().stream().noneMatch(s -> s.getId().equals(subject.getId()))) {
                issues.add("Cette matière n'est pas rattachée à la classe");
            }

            String cleaned = TimetableMatcher.stripTitles(r.professor());
            Collection<Professor> pool = subject != null && !subject.getProfessors().isEmpty()
                    ? subject.getProfessors() : professors;
            Professor prof = cleaned.isBlank() ? null : TimetableMatcher.best(pool,
                    p -> List.of(p.getFirstName() + " " + p.getLastName(), p.getLastName() + " " + p.getFirstName(),
                            p.getLastName()), cleaned, 0.5);
            if (prof == null && subject != null && subject.getProfessors().size() == 1) {
                prof = subject.getProfessors().iterator().next();
            }
            if (prof == null) {
                issues.add("Professeur introuvable" + quote(r.professor()));
            }

            String conflict = null;
            if (issues.isEmpty() && timetableId != null) {
                conflict = timetableService.validateSession(timetableId,
                        new SessionRequest(day, start, end, r.room(), cls.getId(), subject.getId(), prof.getId()));
            }
            rows.add(new ImportDto.Row(id++, day == null ? "" : day.name(), start == null ? "" : start.toString(),
                    end == null ? "" : end.toString(), r.room() == null ? "" : r.room(), nz(r.className()),
                    nz(r.subject()), nz(r.professor()),
                    cls == null ? null : cls.getId(), cls == null ? "" : cls.getName(),
                    subject == null ? null : subject.getId(), subject == null ? "" : subject.getName(),
                    prof == null ? null : prof.getId(), prof == null ? "" : prof.getFirstName() + " " + prof.getLastName(),
                    issues, conflict, issues.isEmpty() && conflict == null));
        }
        return rows;
    }

    private static String quote(String s) {
        return s == null || s.isBlank() ? "" : " : « " + s + " »";
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}