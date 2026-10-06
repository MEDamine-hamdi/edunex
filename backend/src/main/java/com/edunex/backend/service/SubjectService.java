package com.edunex.backend.service;

import com.edunex.backend.dto.SubjectRequest;
import com.edunex.backend.dto.SubjectResponse;
import com.edunex.backend.entity.Professor;
import com.edunex.backend.entity.Subject;
import com.edunex.backend.exception.ConflictException;
import com.edunex.backend.exception.ResourceNotFoundException;
import com.edunex.backend.repository.ProfessorRepository;
import com.edunex.backend.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final ProfessorRepository professorRepository;

    @Transactional
    public SubjectResponse create(SubjectRequest req) {
        if (subjectRepository.findByCode(req.code()).isPresent()) {
            throw new ConflictException("Subject code already in use");
        }
        Subject s = new Subject();
        apply(s, req);
        return toResponse(subjectRepository.save(s));
    }

    @Transactional(readOnly = true)
    public List<SubjectResponse> findAll() {
        return subjectRepository.findAll().stream()
                .sorted(Comparator.comparing(Subject::getName))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SubjectResponse update(Long id, SubjectRequest req) {
        Subject s = find(id);
        subjectRepository.findByCode(req.code())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new ConflictException("Subject code already in use");
                });
        apply(s, req);
        return toResponse(subjectRepository.save(s));
    }

    @Transactional
    public SubjectResponse assignProfessors(Long id, List<Long> professorIds) {
        Subject s = find(id);
        List<Professor> professors = professorRepository.findAllById(professorIds);
        if (professors.size() != new HashSet<>(professorIds).size()) {
            throw new ResourceNotFoundException("One or more professors not found");
        }
        s.setProfessors(new HashSet<>(professors));
        return toResponse(subjectRepository.save(s));
    }

    private Subject find(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
    }

    private void apply(Subject s, SubjectRequest req) {
        s.setName(req.name());
        s.setCode(req.code());
        s.setCoefficient(req.coefficient());
        s.setCredits(req.credits());
    }

    private SubjectResponse toResponse(Subject s) {
        List<SubjectResponse.PersonRef> professors = s.getProfessors().stream()
                .sorted(Comparator.comparing(Professor::getLastName))
                .map(p -> new SubjectResponse.PersonRef(p.getId(), p.getFirstName(), p.getLastName()))
                .toList();
        return new SubjectResponse(s.getId(), s.getName(), s.getCode(),
                s.getCoefficient(), s.getCredits(), professors);
    }
}
