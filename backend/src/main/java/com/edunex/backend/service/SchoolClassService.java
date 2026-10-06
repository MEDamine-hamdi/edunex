package com.edunex.backend.service;

import com.edunex.backend.dto.ClassRequest;
import com.edunex.backend.dto.ClassResponse;
import com.edunex.backend.dto.SectionRequest;
import com.edunex.backend.dto.SectionResponse;
import com.edunex.backend.entity.SchoolClass;
import com.edunex.backend.entity.Section;
import com.edunex.backend.entity.Subject;
import com.edunex.backend.exception.ResourceNotFoundException;
import com.edunex.backend.repository.SchoolClassRepository;
import com.edunex.backend.repository.SectionRepository;
import com.edunex.backend.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SchoolClassService {

    private final SchoolClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final SectionRepository sectionRepository;

    @Transactional
    public ClassResponse create(ClassRequest req) {
        SchoolClass c = new SchoolClass();
        apply(c, req);
        return toResponse(classRepository.save(c));
    }

    @Transactional(readOnly = true)
    public List<ClassResponse> findAll() {
        return classRepository.findAll().stream()
                .sorted(Comparator.comparing(SchoolClass::getName))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ClassResponse update(Long id, ClassRequest req) {
        SchoolClass c = find(id);
        apply(c, req);
        return toResponse(classRepository.save(c));
    }

    @Transactional
    public ClassResponse assignSubjects(Long id, List<Long> subjectIds) {
        SchoolClass c = find(id);
        List<Subject> subjects = subjectRepository.findAllById(subjectIds);
        if (subjects.size() != new HashSet<>(subjectIds).size()) {
            throw new ResourceNotFoundException("One or more subjects not found");
        }
        c.setSubjects(new HashSet<>(subjects));
        return toResponse(classRepository.save(c));
    }

    @Transactional
    public SectionResponse createSection(Long classId, SectionRequest req) {
        SchoolClass c = find(classId);
        Section section = new Section();
        section.setName(req.name());
        section.setSchoolClass(c);
        Section saved = sectionRepository.save(section);
        return new SectionResponse(saved.getId(), saved.getName(), c.getId());
    }

    @Transactional(readOnly = true)
    public List<SectionResponse> findSections(Long classId) {
        find(classId);
        return sectionRepository.findBySchoolClassId(classId).stream()
                .map(s -> new SectionResponse(s.getId(), s.getName(), classId))
                .toList();
    }

    private SchoolClass find(Long id) {
        return classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
    }

    private void apply(SchoolClass c, ClassRequest req) {
        c.setName(req.name());
        c.setLevel(req.level());
        c.setAcademicYear(req.academicYear());
    }

    private ClassResponse toResponse(SchoolClass c) {
        List<ClassResponse.SubjectRef> subjects = c.getSubjects().stream()
                .sorted(Comparator.comparing(Subject::getName))
                .map(s -> new ClassResponse.SubjectRef(s.getId(), s.getName(), s.getCode()))
                .toList();
        return new ClassResponse(c.getId(), c.getName(), c.getLevel(), c.getAcademicYear(), subjects);
    }
}
