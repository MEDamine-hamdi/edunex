package com.edunex.backend.service;

import com.edunex.backend.dto.CreateProfessorRequest;
import com.edunex.backend.dto.ProfessorResponse;
import com.edunex.backend.entity.Professor;
import com.edunex.backend.enums.UserStatus;
import com.edunex.backend.exception.ConflictException;
import com.edunex.backend.exception.ResourceNotFoundException;
import com.edunex.backend.repository.ProfessorRepository;
import com.edunex.backend.repository.RoleRepository;
import com.edunex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfessorService {

    private final ProfessorRepository professorRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AccountCredentialsService credentials;

    @Transactional
    public ProfessorResponse create(CreateProfessorRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ConflictException("Email already in use");
        }
        if (professorRepository.findByEmployeeNumber(req.employeeNumber()).isPresent()) {
            throw new ConflictException("Employee number already in use");
        }

        Professor p = new Professor();
        p.setFirstName(req.firstName());
        p.setLastName(req.lastName());
        p.setEmail(req.email());
        p.setPhone(req.phone());
        p.setEmployeeNumber(req.employeeNumber());
        p.setDepartment(req.department());
        p.setSpecialty(req.specialty());
        p.setRole(roleRepository.findByName("ROLE_PROFESSOR")
                .orElseThrow(() -> new ResourceNotFoundException("Role ROLE_PROFESSOR not found")));
        credentials.issue(p);

        return toResponse(professorRepository.save(p));
    }

    @Transactional(readOnly = true)
    public List<ProfessorResponse> findAll() {
        return professorRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public ProfessorResponse updateStatus(Long id, UserStatus status) {
        Professor p = professorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Professor not found"));
        p.setStatus(status);
        return toResponse(professorRepository.save(p));
    }

    private ProfessorResponse toResponse(Professor p) {
        return new ProfessorResponse(p.getId(), p.getFirstName(), p.getLastName(), p.getEmail(),
                p.getPhone(), p.getEmployeeNumber(), p.getDepartment(), p.getSpecialty(),
                p.getStatus().name());
    }
}
